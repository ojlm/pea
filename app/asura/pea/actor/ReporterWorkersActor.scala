package asura.pea.actor

import java.io.File
import java.nio.charset.StandardCharsets

import akka.Done
import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.model.{ApiCode, ApiRes}
import asura.common.util.{JsonUtils, LogUtils}
import asura.pea.PeaConfig
import asura.pea.actor.ReporterWorkersActor._
import asura.pea.model.ReporterJobStatus.JobWorkerStatus
import asura.pea.model._
import asura.pea.service.PeaService
import org.apache.curator.framework.recipes.cache.{NodeCache, NodeCacheListener}
import org.apache.zookeeper.CreateMode

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

// FIXME: assume that all workers are still idle
class ReporterWorkersActor(workers: Seq[PeaMember]) extends BaseActor {

  implicit val ec = context.dispatcher
  val DEFAULT_TIME_INTERVAL = 10 seconds
  val runId = self.path.name
  val jobNode = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_JOBS}/${runId}"
  // whether worker node was started
  val workerStartedMap = mutable.Map[String, Boolean]()
  val jobStatus = {
    val workersStatus = mutable.Map[String, JobWorkerStatus]()
    workers.foreach(worker => {
      val addressKey = worker.toAddress
      workersStatus += (addressKey -> JobWorkerStatus())
      workerStartedMap += (addressKey -> false)
    })
    ReporterJobStatus(
      status = MemberStatus.REPORTER_RUNNING, // only `running` or `finished`
      runId = runId,
      start = System.currentTimeMillis(),
      end = 0L,
      workers = workersStatus
    )
  }
  val nodeCaches = ArrayBuffer[NodeCache]()

  override def receive: Receive = {
    case msg: ReporterJobStatus =>
      log.debug(s"Current job status change to: ${msg}")
    case JobWorkerStatusChange(worker, memberStatus) =>
      handleWorkerStatusChangeEvent(worker, memberStatus)
    case msg: SingleHttpScenarioMessage =>
      watchWorkersAndSendLoad(msg)
    case msg: RunSimulationMessage =>
      watchWorkersAndSendLoad(msg)
    case DownloadSimulationFinished(worker, _) =>
      jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.REPORTER_WORKER_FINISHED))
      self ! PushStatusToZk
      self ! TryGenerateReport
    case TryGenerateReport =>
      if (jobStatus.workers.forall(s => MemberStatus.isWorkerOver(s._2.status))) {
        jobStatus.end = System.currentTimeMillis()
        jobStatus.status = MemberStatus.REPORTER_REPORTING
        self ! PushStatusToZk
        generateReport()
      }
    case PushStatusToZk =>
      pushJobStatusToZk()
    case TryCheckWorkersIdleStatus => // check whether worker is 'idle' manually
      tryCheckWorkersIdleStatus()
    case msg: Any =>
      log.warning(s"Unsupported message type: ${msg}")
      stopSelf()
  }

  /**
    * worker status flow: 'idle'(initial) -> 'running' -> 'idle'
    */
  def handleWorkerStatusChangeEvent(worker: PeaMember, workerStatus: MemberStatus): Unit = {
    val addressKey = worker.toAddress
    val jobWorkerStatus = this.jobStatus.workers(addressKey)
    if (null != workerStatus) {
      if (!workerStatus.status.equals(jobWorkerStatus.status)) {
        log.debug(
          s"worker(${addressKey}) status: ${workerStatus.status}, " +
            s"start: ${workerStatus.start}, end: ${workerStatus.end}, " +
            s"runId: ${workerStatus.runId}"
        )
        // worker should only be idle
        if (MemberStatus.WORKER_RUNNING.equals(workerStatus.status)) {
          jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.WORKER_RUNNING, null))
        } else if (MemberStatus.WORKER_IDLE.equals(workerStatus.status)) {
          jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.REPORTER_WORKER_GATHERING, workerStatus.errMsg))
          downloadSimulationLog(worker) pipeTo self
        } else {
          // code should not run here
          jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.REPORTER_WORKER_IIL, workerStatus.status))
        }
        self ! PushStatusToZk
      }
    } else {
      jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.REPORTER_WORKER_IIL, "Node unavailable"))
      self ! PushStatusToZk
      self ! TryGenerateReport
    }
  }

  private def dealSendJobResponse(worker: PeaMember, futureRes: Future[ApiRes]): Future[Done] = {
    val addressKey = worker.toAddress
    futureRes.map(res => {
      if (ApiCode.OK.equals(res.code)) {
        jobStatus.workers += (addressKey -> JobWorkerStatus(MemberStatus.WORKER_RUNNING, null))
      } else { // something wrong after idle status checked
        jobStatus.workers += (addressKey -> JobWorkerStatus(MemberStatus.REPORTER_WORKER_IIL, res.msg))
      }
      this.workerStartedMap(addressKey) = true
      Done
    }).recover {
      case t: Throwable =>
        jobStatus.workers += (addressKey -> JobWorkerStatus(MemberStatus.REPORTER_WORKER_IIL, t.getMessage))
        this.workerStartedMap(addressKey) = true
        Done
    }
  }

  def watchWorkersAndSendLoad(load: LoadMessage): Unit = {
    initJobNode()
    val doneFutures = workers.map(worker => load match {
      case msg: SingleHttpScenarioMessage =>
        watchWorkerNode(worker)
        val futureRes = PeaService.sendSingleHttpScenario(worker, msg)
        dealSendJobResponse(worker, futureRes)
      case msg: RunSimulationMessage =>
        watchWorkerNode(worker)
        val futureRes = PeaService.sendSimulation(worker, msg)
        dealSendJobResponse(worker, futureRes)
    })
    Future.sequence(doneFutures).map(_ => {
      self ! PushStatusToZk
      self ! TryGenerateReport // condition when nodes may be ill
      context.system.scheduler.scheduleOnce(DEFAULT_TIME_INTERVAL) {
        self ! TryCheckWorkersIdleStatus
      }
    })
  }

  def watchWorkerNode(worker: PeaMember): Unit = {
    val workerNode = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_WORKERS}/${worker.toNodeName}"
    val nodeCache = new NodeCache(PeaConfig.zkClient, workerNode)
    nodeCache.start()
    nodeCaches += nodeCache
    nodeCache.getListenable.addListener(new NodeCacheListener {
      override def nodeChanged(): Unit = {
        val data = nodeCache.getCurrentData
        if (null != data) {
          val status = JsonUtils.parse(new String(data.getData, StandardCharsets.UTF_8), classOf[MemberStatus])
          if (runId.equals(status.runId)) { // filter events that not belong to this job
            self ! JobWorkerStatusChange(worker, status)
          }
        } else {
          self ! JobWorkerStatusChange(worker, null)
        }
      }
    })
  }

  def downloadSimulationLog(worker: PeaMember): Future[DownloadSimulationFinished] = {
    val futureFile = if (worker.toAddress.equals(PeaMember.toAddress(PeaConfig.address, PeaConfig.port))) {
      // current node
      Future.successful(null)
    } else {
      PeaService.downloadSimulationLog(worker, runId)
    }
    futureFile.map(file => DownloadSimulationFinished(worker, file))
  }

  def generateReport(): Unit = {
    GatlingRunnerActor.generateReport(runId)
      .recover {
        case t: Throwable => log.warning(LogUtils.stackTraceToString(t)); -1
      }
      .map(code => {
        code match {
          case -1 => log.debug("[GenerateReport]:Exception")
          case 0 => log.debug("[GenerateReport]:Success")
          case 1 => log.debug("[GenerateReport]:InvalidArguments")
          case 2 => log.debug("[GenerateReport]:AssertionsFailed")
        }
        jobStatus.status = MemberStatus.REPORTER_FINISHED
        self ! PushStatusToZk
        context.system.scheduler.scheduleOnce(DEFAULT_TIME_INTERVAL) {
          // destroy self after 10 seconds
          stopSelf()
        }
      })
  }

  def initJobNode(): Unit = {
    PeaConfig.zkClient.create()
      .creatingParentsIfNeeded()
      .withMode(CreateMode.EPHEMERAL)
      .forPath(jobNode, JsonUtils.stringify(jobStatus).getBytes(StandardCharsets.UTF_8))
    val nodeCache = new NodeCache(PeaConfig.zkClient, jobNode)
    nodeCache.start()
    nodeCaches += nodeCache
    nodeCache.getListenable.addListener(new NodeCacheListener {
      override def nodeChanged(): Unit = {
        val jobStatus = JsonUtils.parse(
          new String(nodeCache.getCurrentData.getData, StandardCharsets.UTF_8),
          classOf[ReporterJobStatus]
        )
        self ! jobStatus
      }
    })
  }

  def pushJobStatusToZk(): Unit = {
    PeaConfig.zkClient
      .setData()
      .forPath(jobNode, JsonUtils.stringify(jobStatus).getBytes(StandardCharsets.UTF_8))
  }

  // check if all workers are idle, because of undependable zk node cache status
  def tryCheckWorkersIdleStatus(): Unit = {
    val needCheckWorkers = this.workers.filter(member => {
      val addressKey = member.toAddress
      val jobWorkerStatus = this.jobStatus.workers(addressKey)
      !this.workerStartedMap(addressKey) || (this.workerStartedMap(addressKey) && MemberStatus.WORKER_RUNNING.equals(jobWorkerStatus.status))
    })
    if (needCheckWorkers.nonEmpty) {
      val futures = needCheckWorkers.map(member => {
        PeaService.getMemberStatus(member)
          .map(response => {
            if (ApiCode.OK == response.code) {
              self ! JobWorkerStatusChange(member, response.data)
            } else {
              self ! JobWorkerStatusChange(member, MemberStatus(status = MemberStatus.REPORTER_WORKER_IIL, errMsg = response.msg))
            }
          })
          .recover { case t: Throwable =>
            self ! JobWorkerStatusChange(member, MemberStatus(status = MemberStatus.REPORTER_WORKER_IIL, errMsg = t.getMessage))
          }
      })
      Future.sequence(futures).map(_ => {
        context.system.scheduler.scheduleOnce(DEFAULT_TIME_INTERVAL) {
          self ! TryCheckWorkersIdleStatus
        }
      })
    }
  }

  def stopSelf(): Unit = {
    context stop self
  }

  override def postStop(): Unit = {
    try {
      nodeCaches.foreach(_.close())
      PeaConfig.zkClient.delete().guaranteed().forPath(jobNode)
    } catch {
      case t: Throwable => log.warning(LogUtils.stackTraceToString(t))
    }
  }
}

object ReporterWorkersActor {

  def props(workers: Seq[PeaMember]) = Props(new ReporterWorkersActor(workers))

  case class JobWorkerStatusChange(worker: PeaMember, status: MemberStatus)

  case object PushStatusToZk

  case class DownloadSimulationFinished(worker: PeaMember, file: File)

  case object TryGenerateReport

  case object TryCheckWorkersIdleStatus

}
