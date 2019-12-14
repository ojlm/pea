package pea.app.api

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.{Collections, Date}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging
import controllers.Assets
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import pea.app.PeaConfig
import pea.app.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import pea.app.actor.CompilerActor.{AsyncCompileMessage, GetAllSimulations}
import pea.app.actor.ReporterActor.{GetAllWorkers, RunProgramJob, RunScriptJob, SingleHttpScenarioJob}
import pea.app.api.BaseApi.OkApiRes
import pea.app.api.util.ResultUtils
import pea.app.model._
import pea.app.service.PeaService
import pea.common.model.{ApiRes, ApiResError}
import pea.common.util.{JsonUtils, LogUtils}
import play.api.http.HttpErrorHandler
import play.api.mvc._

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeApi @Inject()(
                         implicit val system: ActorSystem,
                         implicit val exec: ExecutionContext,
                         implicit val mat: Materializer,
                         val controllerComponents: SecurityComponents,
                         val assets: Assets,
                         val errorHandler: HttpErrorHandler,
                       ) extends BaseApi with CommonChecks with StrictLogging {

  def index() = assets.at("index.html")

  def asset(resource: String): Action[AnyContent] = if (resource.startsWith("api")) {
    Action.async(r => errorHandler.onClientError(r, NOT_FOUND, "Not found"))
  } else {
    if (resource.contains(".")) assets.at(resource) else index
  }

  def report(path: String) = Action {
    val absolutePath = s"${PeaConfig.resultsFolder}${File.separator}${path}"
    val file = new File(absolutePath)
    if (file.isDirectory) {
      val indexFile = new File(s"${absolutePath}${File.separator}index.html")
      if (indexFile.exists()) {
        TemporaryRedirect(s"/report/${path}/index.html")
      } else {
        ResultUtils.filesEntity(path, file.listFiles())
      }
    } else {
      if (file.exists()) {
        if (file.getCanonicalPath.startsWith(PeaConfig.resultsFolder)) {
          Ok.sendFile(file, true)
        } else {
          OkApiRes(ApiResError(s"Blocking access to this file: ${file.getCanonicalPath}"))
        }
      } else {
        OkApiRes(ApiResError(s"File is not there: ${file.getCanonicalPath}"))
      }
    }
  }

  def reports() = Action {
    val file = new File(PeaConfig.resultsFolder)
    val files = file.listFiles()
    if (null != files) {
      OkApiRes(ApiRes(data = files.filter(_.isDirectory).sortBy(_.lastModified()).reverse.map(file => {
        Map("name" -> file.getName, "last" -> new Date(file.lastModified()))
      })))
    } else {
      OkApiRes(ApiRes(data = Nil))
    }
  }

  def jobs() = Action.async { implicit req =>
    val children = try {
      val jobs: ArrayBuffer[ReporterJobStatus] = ArrayBuffer()
      PeaConfig.zkClient
        .getChildren.forPath(s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_JOBS}")
        .forEach(runId => {
          val job = getJobStatus(runId)
          if (null != job && !MemberStatus.REPORTER_FINISHED.equals(job.status)) {
            jobs += job
          }
        })
      jobs.sortBy(_.start)
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        Nil
    }
    Future.successful(children).toOkResult
  }

  def jobDetail(runId: String) = Action.async { implicit req =>
    val data = getJobStatus(runId)
    val status = if (null != data) data else ReporterJobStatus()
    Future.successful(status).toOkResult
  }

  def workers() = Action.async { implicit req =>
    (PeaConfig.reporterActor ? GetAllWorkers).toOkResult
  }

  def reporters() = Action.async { implicit req =>
    val children = try {
      PeaConfig.zkClient.getChildren.forPath(s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_REPORTERS}")
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        Collections.emptyList[String]()
    }
    Future.successful(children.asScala.map(PeaMember(_)).filter(m => null != m)).toOkResult
  }

  def single() = Action(parse.byteString).async { implicit req =>
    checkReporterEnable {
      val message = req.bodyAs(classOf[SingleHttpScenarioJob])
      loadJob(message)
    }
  }

  def runScript() = Action(parse.byteString).async { implicit req =>
    checkReporterEnable {
      val message = req.bodyAs(classOf[RunScriptJob])
      loadJob(message)
    }
  }

  def runProgram() = Action(parse.byteString).async { implicit req =>
    checkReporterEnable {
      val message = req.bodyAs(classOf[RunProgramJob])
      loadJob(message)
    }
  }

  def simulations() = Action(parse.byteString).async { implicit req =>
    (PeaConfig.workerActor ? GetAllSimulations).toOkResult
  }

  def stop() = Action(parse.byteString).async { implicit req =>
    val message = req.bodyAs(classOf[WorkersRequest])
    PeaService.stopWorkers(message.workers).toOkResult
  }

  def compile() = Action(parse.byteString).async { implicit req =>
    val message = req.bodyAs(classOf[WorkersCompileRequest])
    if (PeaConfig.enableReporter) PeaConfig.workerActor ! AsyncCompileMessage(pull = message.pull)
    PeaService.compileWorkers(message.workers, message.pull).toOkResult
  }

  private def getJobStatus(runId: String): ReporterJobStatus = {
    val path = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_JOBS}/${runId}"
    try {
      val data = PeaConfig.zkClient.getData.forPath(path)
      if (null != data) {
        JsonUtils.parse(new String(data, StandardCharsets.UTF_8), classOf[ReporterJobStatus])
      } else {
        null
      }
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        null
    }
  }

  private def loadJob(message: LoadJob): Future[Result] = {
    val workers = message.workers
    val load = message.load
    val jobs = message.jobs
    if (null == jobs || jobs.isEmpty) {
      if (null == workers || workers.isEmpty) {
        FutureErrorResult("Empty workers")
      } else {
        if (null != load) {
          val exception = load.isValid()
          if (null != exception) {
            Future.failed(exception)
          } else {
            (PeaConfig.reporterActor ? message).toOkResult
          }
        } else {
          FutureErrorResult("Empty load")
        }
      }
    } else {
      if (jobs.forall(job => null == job.load.isValid())) {
        (PeaConfig.reporterActor ? message).toOkResult
      } else {
        FutureErrorResult("Invalid request body")
      }
    }
  }
}
