package asura.pea.api

import java.io.File

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import asura.pea.PeaConfig
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.CompilerActor.AsyncCompileMessage
import asura.pea.actor.WebCompilerMonitorActor.WebCompilerMonitorOptions
import asura.pea.actor.WebResponseMonitorActor.WebResponseMonitorOptions
import asura.pea.actor.WebWorkerMonitorActor.WebWorkerMonitorOptions
import asura.pea.actor.WorkerActor.{GetNodeStatusMessage, StopEngine}
import asura.pea.actor.{WebCompilerMonitorActor, WebResponseMonitorActor, WebWorkerMonitorActor}
import asura.pea.api.BaseApi.OkApiRes
import asura.pea.common.actor.{ActorEvent, SenderMessage}
import asura.pea.common.model.ApiResError
import asura.pea.common.util.{JsonUtils, StringUtils}
import asura.pea.model.job.{RunProgramMessage, RunScriptMessage, SingleHttpScenarioMessage}
import asura.pea.util.SimulationLogUtils
import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Singleton}
import org.pac4j.play.scala.SecurityComponents
import play.api.mvc.WebSocket

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatlingApi @Inject()(
                            implicit val system: ActorSystem,
                            implicit val exec: ExecutionContext,
                            implicit val mat: Materializer,
                            val controllerComponents: SecurityComponents
                          ) extends BaseApi with CommonChecks with StrictLogging {

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

  def runScript() = Action(parse.byteString).async { implicit req =>
    checkWorkerEnable {
      val message = req.bodyAs(classOf[RunScriptMessage])
      val exception = message.isValid()
      if (null != exception) {
        Future.failed(exception)
      } else {
        (PeaConfig.workerActor ? message).toOkResult
      }
    }
  }

  def runProgram() = Action(parse.byteString).async { implicit req =>
    checkWorkerEnable {
      val message = req.bodyAs(classOf[RunProgramMessage])
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

  def response() = WebSocket.acceptOrResult[String, String] { implicit req =>
    Future.successful {
      Right {
        val actorRef = system.actorOf(WebResponseMonitorActor.props())
        stringToActorEventFlow(actorRef, classOf[WebResponseMonitorOptions])
      }
    }
  }

  def getSimulationLog(runId: String) = Action {
    val file = new File(SimulationLogUtils.simulationLogFile(runId))
    logger.debug(s"Downloading file(${file.exists()}): ${file.getCanonicalPath}")
    if (file.exists()) {
      if (file.getCanonicalPath.startsWith(PeaConfig.resultsFolder)) {
        Ok.sendFile(file, false)
      } else {
        OkApiRes(ApiResError(s"Blocking access to this file: ${file.getCanonicalPath}"))
      }
    } else {
      OkApiRes(ApiResError(s"File is not there: ${file.getCanonicalPath}"))
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
}
