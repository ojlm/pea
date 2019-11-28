package asura.pea.actor

import java.nio.charset.StandardCharsets

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.JsonUtils
import asura.pea.PeaConfig
import asura.pea.actor.ReporterActor._
import asura.pea.model._
import asura.pea.model.job.{RunProgramSingleJob, RunScriptMessage, SingleHttpScenarioMessage}
import asura.pea.service.PeaService
import asura.pea.service.PeaService.WorkersAvailable
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class ReporterActor extends BaseActor {

  implicit val ec = context.dispatcher

  var workersCache: PathChildrenCache = null
  watchAllWorkers()

  override def receive: Receive = {
    case SingleHttpScenarioJob(workers, request) =>
      checkAndStartJob(workers, request) pipeTo sender()
    case RunScriptJob(workers, request) =>
      checkAndStartJob(workers, request) pipeTo sender()
    case msg: RunProgramJob =>
      checkAndStartJobs(msg) pipeTo sender()
    case GetAllWorkers =>
      sender() ! getWorkersData()
    case message: Any =>
      log.warning(s"Unsupported message: ${message}")
  }

  private def watchAllWorkers(): Unit = {
    val path = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_WORKERS}"
    this.workersCache = new PathChildrenCache(PeaConfig.zkClient, path, true)
    this.workersCache.start(StartMode.BUILD_INITIAL_CACHE)
  }

  private def getWorkersData(): Seq[WorkerData] = {
    val items = ArrayBuffer[WorkerData]()
    this.workersCache.getCurrentData.forEach(childData => {
      val nodeIndex = PeaConfig.zkRootPath.length + PeaConfig.PATH_WORKERS.length + 2
      val member = PeaMember(childData.getPath.substring(nodeIndex))
      val data = childData.getData
      val status = if (null != data) {
        JsonUtils.parse(new String(data, StandardCharsets.UTF_8), classOf[MemberStatus])
      } else {
        null
      }
      items += WorkerData(member, status)
    })
    items
  }

  private def checkAndStartJob(
                                workers: Seq[PeaMember],
                                message: LoadMessage
                              ): Future[WorkersAvailable] = {
    PeaService.isWorkersAvailable(workers)
      .map(res => {
        if (res.available) {
          val start = System.currentTimeMillis()
          message.simulationId = PeaConfig.hostname
          message.start = start
          res.runId = PeaService.generateRunId(PeaConfig.hostname, start)
          context.actorOf(ReporterWorkersActor.props(workers), res.runId) ! message
        }
        res
      })
  }

  private def checkAndStartJobs(message: LoadJob): Future[WorkersAvailable] = {
    val workers = message.jobs.map(_.worker)
    PeaService.isWorkersAvailable(workers)
      .map(res => {
        if (res.available) {
          val start = System.currentTimeMillis()
          message.simulationId = PeaConfig.hostname
          message.start = start
          res.runId = PeaService.generateRunId(PeaConfig.hostname, start)
          context.actorOf(ReporterWorkersActor.props(workers), res.runId) ! message
        }
        res
      })
  }

  override def postStop(): Unit = {
    super.postStop()
    if (null != this.workersCache) this.workersCache.close()
  }
}

object ReporterActor {

  def props() = Props(new ReporterActor())

  case class SingleHttpScenarioJob(
                                    override val workers: Seq[PeaMember],
                                    override val request: SingleHttpScenarioMessage,
                                  ) extends LoadJob

  case class RunScriptJob(
                           override val workers: Seq[PeaMember],
                           override val request: RunScriptMessage,
                         ) extends LoadJob

  case class RunProgramJob(
                            override val jobs: Seq[RunProgramSingleJob]
                          ) extends LoadJob {
    val `type`: String = LoadTypes.PROGRAM
  }

  case object GetAllWorkers

  case class WorkerData(member: PeaMember, status: MemberStatus)

}
