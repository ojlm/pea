package asura.pea.actor

import java.nio.charset.StandardCharsets

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.common.model.ApiCode
import asura.common.util.JsonUtils
import asura.pea.PeaConfig
import asura.pea.actor.ReporterWorkersActor.{JobWorkerStatusChange, TryGenerateReport}
import asura.pea.model.ReporterJobStatus.JobWorkerStatus
import asura.pea.model._
import asura.pea.service.PeaService
import org.apache.curator.framework.recipes.cache.{NodeCache, NodeCacheListener}
import org.apache.zookeeper.CreateMode

import scala.collection.mutable

// FIXME: assume that all workers are still idle
class ReporterWorkersActor(workers: Seq[PeaMember]) extends BaseActor {

  implicit val ec = context.dispatcher
  val runId = self.path.name
  val jobNode = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_JOBS}/${runId}"
  val jobStatus = {
    val workersStatus = mutable.Map[String, JobWorkerStatus]()
    workers.foreach(worker => workersStatus += (worker.toAddress -> JobWorkerStatus()))
    ReporterJobStatus(
      status = MemberStatus.RUNNING, // only `running` or `finished`
      runId = runId,
      start = System.currentTimeMillis(),
      end = 0L,
      workers = workersStatus
    )
  }

  override def receive: Receive = {
    case msg: ReporterJobStatus =>
      log.debug(s"Current job status change to: ${msg}")
    case JobWorkerStatusChange(worker, memberStatus) => // worker status change event
      handleWorkerStatusChangeEvent(worker, memberStatus)
    case msg: SingleHttpScenarioMessage =>
      watchWorkersAndSendLoad(msg)
    case TryGenerateReport =>
      tryGenerateReport()
    case _ =>
  }

  def handleWorkerStatusChangeEvent(worker: PeaMember, workerStatus: MemberStatus): Unit = {
    // worker should only be idle
    if (MemberStatus.IDLE.equals(workerStatus.status)) {
      jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.GATHERING, workerStatus.errMsg))
      gatherSimulationLog(worker)
    } else {
      jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.IIL, workerStatus.status))
    }
    pushJobStatusToZk()
  }

  def watchWorkersAndSendLoad(load: LoadMessage): Unit = {
    initJobNode()
    workers.foreach(worker => load match {
      case msg: SingleHttpScenarioMessage =>
        watchWorkerNode(worker)
        PeaService.sendSingleHttpScenario(worker, msg).map(res => {
          if (!ApiCode.OK.equals(res.code)) { // something wrong after idle status checked
            jobStatus.workers += (worker.toAddress -> JobWorkerStatus(MemberStatus.IIL, res.msg))
          }
        })
      case _ =>
        context stop self
    })
  }

  def watchWorkerNode(worker: PeaMember): Unit = {
    val workerNode = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_WORKERS}/${worker.toNodeName}"
    val nodeCache = new NodeCache(PeaConfig.zkClient, workerNode)
    nodeCache.start()
    nodeCache.getListenable.addListener(new NodeCacheListener {
      override def nodeChanged(): Unit = {
        val memberStatus = JsonUtils.parse(
          new String(nodeCache.getCurrentData.getData, StandardCharsets.UTF_8),
          classOf[MemberStatus]
        )
        self ! JobWorkerStatusChange(worker, memberStatus)
      }
    })
  }

  def gatherSimulationLog(worker: PeaMember): Unit = {
    // TODO:
    // downloading log file, filter current node
    // update worker status in job node,
    // check weather finished, send TryGenerateReport
  }

  def tryGenerateReport(): Unit = {
    // TODO
  }

  def initJobNode(): Unit = {
    PeaConfig.zkClient.create()
      .creatingParentsIfNeeded()
      .withMode(CreateMode.EPHEMERAL)
      .forPath(jobNode, JsonUtils.stringify(jobStatus).getBytes(StandardCharsets.UTF_8))
    val nodeCache = new NodeCache(PeaConfig.zkClient, jobNode)
    nodeCache.start()
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

  override def postStop(): Unit = {
    PeaConfig.zkClient.delete().guaranteed().forPath(jobNode)
  }
}

object ReporterWorkersActor {

  def props(workers: Seq[PeaMember]) = Props(new ReporterWorkersActor(workers))

  case class JobWorkerStatusChange(worker: PeaMember, status: MemberStatus)

  case object TryGenerateReport

}
