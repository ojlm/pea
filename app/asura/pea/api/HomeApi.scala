package asura.pea.api

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import asura.common.util.FutureUtils
import asura.pea.PeaConfig
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.PeaReporterActor.SingleHttpScenarioJob
import asura.pea.model.PeaMember
import asura.play.api.BaseApi
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HomeApi @Inject()(
                         implicit val system: ActorSystem,
                         implicit val exec: ExecutionContext,
                         implicit val mat: Materializer,
                         val controllerComponents: SecurityComponents
                       ) extends BaseApi {

  val peaReporter = PeaConfig.reporterActor

  def index() = Action.async { implicit req =>
    Future.successful(Ok("pea"))
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
    val message = req.bodyAs(classOf[SingleHttpScenarioJob])
    val workers = message.workers
    val request = message.request
    if (null == workers || workers.isEmpty) {
      FutureUtils.illegalArgs("Empty workers")
    } else {
      if (null != request) {
        val exception = request.isValid()
        if (null != exception) {
          Future.failed(exception)
        } else {
          (peaReporter ? message).toOkResult
        }
      } else {
        FutureUtils.illegalArgs("Empty request")
      }
    }
  }
}
