package asura.pea.api

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.{Collections, Date}

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import asura.common.model.{ApiRes, ApiResError}
import asura.common.util.{JsonUtils, LogUtils}
import asura.pea.PeaConfig
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.CompilerActor.{AsyncCompileMessage, GetAllSimulations}
import asura.pea.actor.ReporterActor.{GetAllWorkers, RunProgramJob, RunScriptJob, SingleHttpScenarioJob}
import asura.pea.api.util.ResultUtils
import asura.pea.model.{LoadJob, PeaMember, ReporterJobStatus, WorkersRequest}
import asura.pea.service.PeaService
import asura.play.api.BaseApi
import asura.play.api.BaseApi.OkApiRes
import com.typesafe.scalalogging.StrictLogging
import controllers.Assets
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.http.HttpErrorHandler
import play.api.mvc._

import scala.collection.JavaConverters._
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
      PeaConfig.zkClient.getChildren.forPath(s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_JOBS}")
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        Nil
    }
    Future.successful(children).toOkResult
  }

  def jobDetail(runId: String) = Action.async { implicit req =>
    val status = try {
      val path = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_JOBS}/${runId}"
      val data = PeaConfig.zkClient.getData.forPath(path)
      JsonUtils.parse(
        new String(data, StandardCharsets.UTF_8),
        classOf[ReporterJobStatus]
      )
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        ReporterJobStatus()
    }
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
    val message = req.bodyAs(classOf[WorkersRequest])
    if (PeaConfig.enableReporter) PeaConfig.workerActor ! AsyncCompileMessage(pull = true)
    PeaService.compileWorkers(message.workers).toOkResult
  }

  private def loadJob(message: LoadJob): Future[Result] = {
    val workers = message.workers
    val request = message.request
    val jobs = message.jobs
    if (null == jobs || jobs.isEmpty) {
      if (null == workers || workers.isEmpty) {
        FutureErrorResult("Empty workers")
      } else {
        if (null != request) {
          val exception = request.isValid()
          if (null != exception) {
            Future.failed(exception)
          } else {
            (PeaConfig.reporterActor ? message).toOkResult
          }
        } else {
          FutureErrorResult("Empty request")
        }
      }
    } else {
      if (jobs.forall(job => null == job.request.isValid())) {
        (PeaConfig.reporterActor ? message).toOkResult
      } else {
        FutureErrorResult("Invalid request body")
      }
    }
  }
}
