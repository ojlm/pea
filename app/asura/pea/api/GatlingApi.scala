package asura.pea.api

import java.io.File

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import asura.common.actor.{ActorEvent, SenderMessage}
import asura.common.util.{JsonUtils, StringUtils}
import asura.pea.PeaConfig
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.PeaWebMonitorActor
import asura.pea.actor.PeaWebMonitorActor.WebMonitorController
import asura.pea.actor.PeaWorkerActor.{GetNodeStatusMessage, StopEngine}
import asura.pea.actor.ZincCompilerActor.GetAllSimulations
import asura.pea.model.SingleHttpScenarioMessage
import asura.play.api.BaseApi
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.mvc.{Result, WebSocket}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatlingApi @Inject()(
                            implicit val system: ActorSystem,
                            implicit val exec: ExecutionContext,
                            implicit val mat: Materializer,
                            val controllerComponents: SecurityComponents
                          ) extends BaseApi with CommonFunctions {

  def stop() = Action.async { implicit req =>
    checkWorkerEnable {
      (PeaConfig.workerActor ? StopEngine).toOkResult
    }
  }

  def status() = Action.async { implicit req =>
    checkWorkerEnable {
      (PeaConfig.workerActor ? GetNodeStatusMessage).toOkResult
    }
  }

  def single() = Action(parse.byteString).async { implicit req =>
    checkWorkerEnable {
      val message = req.bodyAs(classOf[SingleHttpScenarioMessage])
      val exception = message.isValid()
      if (null != exception) {
        Future.failed(exception)
      } else {
        (PeaConfig.workerActor ? message).toOkResult
      }
    }
  }

  def simulations() = Action(parse.byteString).async { implicit req =>
    (PeaConfig.workerActor ? GetAllSimulations).toOkResult
  }

  def monitor() = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      Right {
        val actorRef = system.actorOf(PeaWebMonitorActor.props())
        stringToActorEventFlow(actorRef, classOf[WebMonitorController])
      }
    }
  }

  def simulation(runId: String) = Action {
    Ok.sendFile(new File(s"${PeaConfig.resultsFolder}/${runId}/simulation.log"), false)
  }

  def stringToActorEventFlow[T <: AnyRef](workActor: ActorRef, msgClass: Class[T]): Flow[String, String, NotUsed] = {
    val incomingMessages: Sink[String, NotUsed] =
      Flow[String].map {
        case text: String => JsonUtils.parse(text, msgClass)
      }.to(Sink.actorRef[T](workActor, PoisonPill))
    val outgoingMessages: Source[String, NotUsed] =
      Source.actorRef[ActorEvent](PeaConfig.DEFAULT_WS_ACTOR_BUFFER_SIZE, OverflowStrategy.dropHead)
        .mapMaterializedValue { outActor =>
          workActor ! SenderMessage(outActor)
          NotUsed
        }
        .map(result => JsonUtils.stringify(result))
        .keepAlive(PeaConfig.KEEP_ALIVE_INTERVAL seconds, () => StringUtils.EMPTY)
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }

  def checkWorkerEnable(func: => Future[Result]): Future[Result] = {
    if (PeaConfig.enableWorker) {
      func
    } else {
      FutureErrorResult("Role worker is disabled")
    }
  }
}
