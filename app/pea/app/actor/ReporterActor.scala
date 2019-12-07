package pea.app.actor

import java.nio.charset.StandardCharsets

import akka.actor.Props
import akka.pattern.pipe
import org.apache.curator.framework.recipes.cache.PathChildrenCache
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode
import pea.app.PeaConfig
import pea.app.actor.ReporterActor._
import pea.app.model._
import pea.app.model.job._
import pea.app.service.PeaService
import pea.app.service.PeaService.WorkersAvailable
import pea.common.actor.BaseActor
import pea.common.util.JsonUtils

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class ReporterActor extends BaseActor {

  implicit val ec = context.dispatcher

  var workersCache: PathChildrenCache = null
  watchAllWorkers()

  override def receive: Receive = {
    case msg: SingleHttpScenarioJob =>
      checkAndStartWorkers(msg) pipeTo sender()
    case msg: RunScriptJob =>
      checkAndStartWorkers(msg) pipeTo sender()
    case msg: RunProgramJob =>
      checkAndStartWorkers(msg) pipeTo sender()
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

  private def checkAndStartWorkers(message: LoadJob): Future[WorkersAvailable] = {
    val workers = if (null != message.workers && message.workers.nonEmpty) {
      message.workers
    } else {
      message.jobs.map(_.worker)
    }
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
                                    override val load: SingleHttpScenarioMessage,
                                    override val jobs: Seq[SingleHttpScenarioSingleJob],
                                  ) extends LoadJob {
    val `type`: String = LoadTypes.SINGLE
  }

  case class RunScriptJob(
                           override val workers: Seq[PeaMember],
                           override val load: RunScriptMessage,
                           override val jobs: Seq[RunScriptSingleJob],
                         ) extends LoadJob {
    val `type`: String = LoadTypes.SCRIPT
  }

  case class RunProgramJob(
                            override val workers: Seq[PeaMember],
                            override val load: RunProgramMessage,
                            override val jobs: Seq[RunProgramSingleJob]
                          ) extends LoadJob {
    val `type`: String = LoadTypes.PROGRAM
  }

  case object GetAllWorkers

  case class WorkerData(member: PeaMember, status: MemberStatus)

}
