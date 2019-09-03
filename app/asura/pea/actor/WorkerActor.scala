package asura.pea.actor

import java.nio.charset.StandardCharsets
import java.util.Date

import akka.actor.{Cancellable, Props}
import akka.pattern.{ask, pipe}
import asura.common.actor.BaseActor
import asura.common.util.{JsonUtils, StringUtils}
import asura.pea.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import asura.pea.actor.CompilerActor.{AsyncCompileMessage, CompileMessage, GetAllSimulations, SimulationValidateMessage}
import asura.pea.actor.GatlingRunnerActor.PeaGatlingRunResult
import asura.pea.actor.WorkerActor._
import asura.pea.model.{LoadMessage, MemberStatus, RunSimulationMessage, SingleHttpScenarioMessage}
import asura.pea.{ErrorMessages, PeaConfig}

import scala.concurrent.Future

class WorkerActor extends BaseActor {

  var memberStatus = MemberStatus(MemberStatus.WORKER_IDLE)

  implicit val ec = context.dispatcher
  val compilerActor = context.actorOf(CompilerActor.props())
  val gatlingRunnerActor = context.actorOf(GatlingRunnerActor.props())
  var engineCancelable: Cancellable = null

  override def receive: Receive = {
    case msg: MemberStatus => // send from listener
      log.debug(s"Current node data change to: ${msg}")
    case GetNodeStatusMessage =>
      sender() ! memberStatus
    case GetAllSimulations =>
      (compilerActor ? GetAllSimulations) pipeTo sender()
    case msg: CompileMessage | AsyncCompileMessage =>
      (compilerActor ? msg) pipeTo sender()
    case msg: SingleHttpScenarioMessage =>
      doSingleHttpScenario(msg) pipeTo sender()
    case msg: RunSimulationMessage =>
      runSimulation(msg) pipeTo sender()
    case StopEngine =>
      sender() ! tryStopEngine()
    case UpdateRunningStatus(runId) =>
      updateRunningStatus(runId)
    case UpdateCodeStatus(code, errMsg) =>
      updateCodeStatus(code, errMsg)
    case UpdateEndStatus(code, errMsg) =>
      updateEndStatus(code, errMsg)
    case _ =>
      ErrorMessages.error_InvalidRequestParameters.toFutureFail pipeTo sender()
  }

  def tryStopEngine(): Boolean = {
    var result = true
    if (null != engineCancelable && !MemberStatus.WORKER_IDLE.equals(memberStatus.status)) {
      if (!engineCancelable.isCancelled) {
        if (engineCancelable.cancel()) {
          self ! UpdateEndStatus(-1, "canceled")
        } else {
          result = false
          self ! UpdateCodeStatus(-1, "cancel failed")
        }
      }
    }
    result
  }

  def runSimulation(message: RunSimulationMessage): Future[String] = {
    if (MemberStatus.WORKER_IDLE.equals(memberStatus.status)) {
      (compilerActor ? SimulationValidateMessage(message.simulation)).flatMap(res => {
        if (res.asInstanceOf[Boolean]) {
          runLoad(message)
        } else {
          Future.failed(new RuntimeException(s"Simulation not compiled: ${message.simulation}"))
        }
      })
    } else {
      ErrorMessages.error_BusyStatus.toFutureFail
    }
  }

  def doSingleHttpScenario(message: SingleHttpScenarioMessage): Future[String] = {
    if (MemberStatus.WORKER_IDLE.equals(memberStatus.status)) {
      asura.pea.singleHttpScenario = message
      runLoad(message)
    } else {
      ErrorMessages.error_BusyStatus.toFutureFail
    }
  }

  private def runLoad(message: LoadMessage): Future[String] = {
    val futureRunResult = (gatlingRunnerActor ? message).asInstanceOf[Future[PeaGatlingRunResult]]
    futureRunResult.map(runResult => {
      engineCancelable = runResult.cancel
      self ! UpdateRunningStatus(runResult.runId)
      runResult.result.map(result => {
        if (!result.isByCanceled) { // stop not by hand
          self ! UpdateEndStatus(result.code, result.errMsg)
        }
      })
      runResult.runId
    })
  }

  private def updateCodeStatus(code: Int, errMsg: String): Unit = {
    memberStatus.code = code
    memberStatus.errMsg = errMsg
    pushToZk()
  }

  private def updateEndStatus(code: Int, errMsg: String): Unit = {
    memberStatus.status = MemberStatus.WORKER_IDLE
    memberStatus.end = new Date().getTime
    memberStatus.code = code
    memberStatus.errMsg = errMsg
    pushToZk()
  }

  private def updateRunningStatus(runId: String): Unit = {
    memberStatus.status = MemberStatus.WORKER_RUNNING
    memberStatus.runId = runId
    memberStatus.start = new Date().getTime
    memberStatus.end = 0L
    memberStatus.code = 0
    memberStatus.errMsg = StringUtils.EMPTY
    pushToZk()
  }

  private def pushToZk(): Unit = {
    PeaConfig.zkClient
      .setData()
      .forPath(PeaConfig.zkCurrWorkerPath, JsonUtils.stringify(memberStatus).getBytes(StandardCharsets.UTF_8))
  }
}

object WorkerActor {

  def props() = Props(new WorkerActor())

  case object GetNodeStatusMessage

  case object StopEngine

  case class UpdateCodeStatus(code: Int, errMsg: String)

  case class UpdateRunningStatus(runId: String)

  case class UpdateEndStatus(code: Int, errMsg: String)

}
