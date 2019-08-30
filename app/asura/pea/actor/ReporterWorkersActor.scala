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
import asura.pea.actor.ReporterWorkersActor.{DownloadSimulationFinished, GenerateReport, JobWorkerStatusChange, PushStatusToZk}
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
// TODO: WatchDog for worker status
class ReporterWorkersActor(workers: Seq[PeaMember]) extends BaseActor {

  implicit val ec = context.dispatcher
  val runId = self.path.name
  val jobNode = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_JOBS}/${runId}"
  val jobStatus = {
    val workersStatus = mutable.Map[String, JobWorkerStatus]()
    workers.foreach(worker => workersStatus += (worker.toAddress -> JobWorkerStatus()))
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
      if (jobStatus.workers.forall(s => MemberStatus.isWorkerOver(s._2.status))) {
        jobStatus.end = System.currentTimeMillis()
        jobStatus.status = MemberStatus.REPORTER_REPORTING
        self ! GenerateReport
      }
      self ! PushStatusToZk
    case GenerateReport =>
      generateReport()
    case PushStatusToZk =>
      pushJobStatusToZk()
    case msg: Any =>
      log.warning(s"Unsupported message type: ${msg}")
      stopSelf()
  }

  /**
    * worker status flow: 'idle'(initial) -> 'running' -> 'idle'
    */
  def handleWorkerStatusChangeEvent(worker: PeaMember, workerStatus: MemberStatus): Unit = {
    log.debug(
      s"worker(${worker.toAddress}) status: ${workerStatus.status}, " +
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

  private def dealServiceResponse(worker: PeaMember, futureRes: Future[ApiRes]): Future[Done] = {
    futureRes.map(res => {
      if (ApiCode.OK.equals(res.code)) { // something wrong after idle status checked
        jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.WORKER_RUNNING, null))
      } else {
        jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.REPORTER_WORKER_IIL, res.msg))
      }
      Done
    }).recover {
      case t: Throwable =>
        jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.REPORTER_WORKER_IIL, t.getMessage))
        Done
    }
  }

  def watchWorkersAndSendLoad(load: LoadMessage): Unit = {
    initJobNode()
    val doneFutures = workers.map(worker => load match {
      case msg: SingleHttpScenarioMessage =>
        watchWorkerNode(worker)
        val futureRes = PeaService.sendSingleHttpScenario(worker, msg)
        dealServiceResponse(worker, futureRes)
      case msg: RunSimulationMessage =>
        watchWorkerNode(worker)
        val futureRes = PeaService.sendSimulation(worker, msg)
        dealServiceResponse(worker, futureRes)
    })
    Future.sequence(doneFutures).map(_ => self ! PushStatusToZk)
  }

  def watchWorkerNode(worker: PeaMember): Unit = {
    val workerNode = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_WORKERS}/${worker.toNodeName}"
    val nodeCache = new NodeCache(PeaConfig.zkClient, workerNode)
    nodeCache.start()
    nodeCaches += nodeCache
    nodeCache.getListenable.addListener(new NodeCacheListener {
      override def nodeChanged(): Unit = {
        val memberStatus = JsonUtils.parse(
          new String(nodeCache.getCurrentData.getData, StandardCharsets.UTF_8),
          classOf[MemberStatus]
        )
        if (runId.equals(memberStatus.runId)) { // filter events that not belong to this job
          self ! JobWorkerStatusChange(worker, memberStatus)
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
        context.system.scheduler.scheduleOnce(10 seconds) {
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

  case object GenerateReport

}
