package asura.pea.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import asura.pea.PeaConfig
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.PeaReporterActor.SingleHttpScenarioJob
import asura.pea.model.PeaMember
import asura.play.api.BaseApi
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
                       ) extends BaseApi with CommonFunctions {

  def index() = assets.at("index.html")

  def asset(resource: String): Action[AnyContent] = if (resource.startsWith("api")) {
    Action.async(r => errorHandler.onClientError(r, NOT_FOUND, "Not found"))
  } else {
    if (resource.contains(".")) assets.at(resource) else index
  }

  def workers() = Action.async { implicit req =>
    val children = PeaConfig.zkClient.getChildren.forPath(s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_WORKERS}")
    Future.successful(children.asScala.map(PeaMember(_)).filter(m => null != m)).toOkResult
  }

  def reporters() = Action.async { implicit req =>
    val children = PeaConfig.zkClient.getChildren.forPath(s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_REPORTERS}")
    Future.successful(children.asScala.map(PeaMember(_)).filter(m => null != m)).toOkResult
  }

  def single() = Action(parse.byteString).async { implicit req =>
    checkReporterEnable {
      val message = req.bodyAs(classOf[SingleHttpScenarioJob])
      val workers = message.workers
      val request = message.request
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
    }
  }

  def checkReporterEnable(func: => Future[Result]): Future[Result] = {
    if (PeaConfig.enableReporter) {
      func
    } else {
      FutureErrorResult("Role reporter is disabled")
    }
  }
}
