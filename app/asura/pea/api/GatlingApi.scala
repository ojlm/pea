package asura.pea.api

import java.io.File

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import asura.common.actor.{ActorEvent, SenderMessage}
import asura.common.model.ApiResError
import asura.common.util.{JsonUtils, StringUtils}
import asura.pea.PeaConfig
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.CompilerActor.AsyncCompileMessage
import asura.pea.actor.WebCompilerMonitorActor.WebCompilerMonitorOptions
import asura.pea.actor.WebWorkerMonitorActor.WebWorkerMonitorOptions
import asura.pea.actor.WorkerActor.{GetNodeStatusMessage, StopEngine}
import asura.pea.actor.{WebCompilerMonitorActor, WebWorkerMonitorActor}
import asura.pea.model.{RunSimulationMessage, SingleHttpScenarioMessage}
import asura.play.api.BaseApi
import asura.play.api.BaseApi.OkApiRes
import com.typesafe.scalalogging.StrictLogging
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
                          ) extends BaseApi with CommonFunctions with StrictLogging {

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

  def runSimulation() = Action(parse.byteString).async { implicit req =>
    checkWorkerEnable {
      val message = req.bodyAs(classOf[RunSimulationMessage])
      val exception = message.isValid()
      if (null != exception) {
        Future.failed(exception)
      } else {
        (PeaConfig.workerActor ? message).toOkResult
      }
    }
  }

  def monitor() = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      Right {
        val actorRef = system.actorOf(WebWorkerMonitorActor.props())
        stringToActorEventFlow(actorRef, classOf[WebWorkerMonitorOptions])
      }
    }
  }

  def compile() = Action(parse.byteString).async { implicit req =>
    checkWorkerEnable {
      val message = req.bodyAs(classOf[AsyncCompileMessage])
      (PeaConfig.workerActor ? message).toOkResult
    }
  }

  def compiler() = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      Right {
        val actorRef = system.actorOf(WebCompilerMonitorActor.props())
        stringToActorEventFlow(actorRef, classOf[WebCompilerMonitorOptions])
      }
    }
  }

  def getSimulationLog(runId: String) = Action {
    val file = new File(s"${PeaConfig.resultsFolder}${File.separator}${runId}${File.separator}simulation.log")
    logger.debug(s"Downloading file(${file.exists()}): ${file.getAbsolutePath}")
    if (file.exists()) {
      if (file.getAbsolutePath.startsWith(PeaConfig.resultsFolder)) {
        Ok.sendFile(file, false)
      } else {
        OkApiRes(ApiResError(s"Blocking access to this file: ${file.getAbsolutePath}"))
      }
    } else {
      OkApiRes(ApiResError(s"File is not there: ${file.getAbsolutePath}"))
    }
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
