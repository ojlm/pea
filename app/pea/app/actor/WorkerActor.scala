package pea.app.actor

import java.nio.charset.StandardCharsets
import java.util.Date

import akka.actor.{Cancellable, Props}
import akka.pattern.{ask, pipe}
import org.apache.curator.framework.recipes.cache.{NodeCache, NodeCacheListener}
import org.apache.zookeeper.CreateMode
import pea.app.PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT
import pea.app.actor.CompilerActor._
import pea.app.actor.GatlingRunnerActor.PeaGatlingRunResult
import pea.app.actor.ProgramRunnerActor.ProgramResult
import pea.app.actor.WorkerActor._
import pea.app.model._
import pea.app.model.job.{RunProgramMessage, RunScriptMessage, SingleHttpScenarioMessage}
import pea.app.{ErrorMessages, PeaConfig}
import pea.common.actor.BaseActor
import pea.common.util.{JsonUtils, StringUtils}

import scala.concurrent.Future
import scala.concurrent.duration._

class WorkerActor extends BaseActor {

  var memberStatus = MemberStatus(MemberStatus.WORKER_IDLE)

  implicit val ec = context.dispatcher
  val compilerActor = context.actorOf(CompilerActor.props())
  val gatlingRunnerActor = context.actorOf(GatlingRunnerActor.props())
  val programRunnerActor = context.actorOf(ProgramRunnerActor.props())
  var nodeCache: NodeCache = null
  var isRegistering = false
  var engineCancelable: Cancellable = null

  override def receive: Receive = {
    case WatchSelf =>
      watchSelfNode()
    case TryReWatchSelf =>
      tryReWatchSelfNode()
    case GetNodeStatusMessage =>
      sender() ! memberStatus
    case GetAllSimulations =>
      (compilerActor ? GetAllSimulations) pipeTo sender()
    case msg: CompileMessage =>
      (compilerActor ? msg) pipeTo sender()
    case msg: SingleHttpScenarioMessage =>
      doSingleHttpScenario(msg) pipeTo sender()
    case msg: RunScriptMessage =>
      runScript(msg) pipeTo sender()
    case msg: RunProgramMessage =>
      runProgram(msg) pipeTo sender()
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

  def runProgram(message: RunProgramMessage): Future[String] = {
    if (MemberStatus.WORKER_IDLE.equals(memberStatus.status)) {
      val futureRunResult = (programRunnerActor ? message).asInstanceOf[Future[ProgramResult]]
      futureRunResult.map(runResult => {
        engineCancelable = new Cancellable {
          override def cancel(): Boolean = {
            self ! UpdateEndStatus(-1, "Program is canceled")
            runResult.cancel.cancel()
          }

          override def isCancelled: Boolean = false
        }
        self ! UpdateRunningStatus(runResult.runId)
        runResult.result.map(code => {
          self ! UpdateEndStatus(code, null)
        }).recover {
          case t: Throwable => t.getMessage
        }
        runResult.runId
      })
    } else {
      ErrorMessages.error_BusyStatus.toFutureFail
    }
  }

  def runScript(message: RunScriptMessage): Future[String] = {
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
      pea.app.singleHttpScenario = message
      runLoad(message)
    } else {
      ErrorMessages.error_BusyStatus.toFutureFail
    }
  }

  private def runLoad(message: LoadMessage): Future[String] = {
    val futureRunResult = (gatlingRunnerActor ? message).asInstanceOf[Future[PeaGatlingRunResult]]
    futureRunResult.map(runResult => {
      if (null == runResult.error) {
        engineCancelable = runResult.cancel
        self ! UpdateRunningStatus(runResult.runId)
        runResult.result.map(result => {
          if (!result.isByCanceled) { // stop not by hand
            self ! UpdateEndStatus(result.code, result.errMsg)
          }
        })
        runResult.runId
      } else {
        self ! UpdateEndStatus(-1, runResult.error.getMessage)
        // throw the error to the controller
        throw runResult.error
      }
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
    memberStatus.oshi = OshiInfo.getOshiInfo()
    PeaConfig.zkClient
      .setData()
      .forPath(PeaConfig.zkCurrWorkerPath, JsonUtils.stringify(memberStatus).getBytes(StandardCharsets.UTF_8))
  }

  private def tryReWatchSelfNode(): Unit = {
    if (!isRegistering) {
      isRegistering = true
      val stat = PeaConfig.zkClient.checkExists().forPath(PeaConfig.zkCurrWorkerPath)
      if (null == stat) {
        watchSelfNode()
      }
      isRegistering = false
    }
  }

  private def watchSelfNode(): Unit = {
    val nodeData = JsonUtils.stringify(this.memberStatus).getBytes(StandardCharsets.UTF_8)
    PeaConfig.zkClient.create()
      .creatingParentsIfNeeded()
      .withMode(CreateMode.EPHEMERAL)
      .forPath(PeaConfig.zkCurrWorkerPath, nodeData)
    if (nodeCache == null) {
      nodeCache = new NodeCache(PeaConfig.zkClient, PeaConfig.zkCurrWorkerPath)
      nodeCache.start(true)
      nodeCache.getListenable.addListener(new NodeCacheListener {
        override def nodeChanged(): Unit = {
          val currentData = nodeCache.getCurrentData
          if (null != currentData) {
            val nodeData = new String(currentData.getData, StandardCharsets.UTF_8)
            val memberStatus = JsonUtils.parse(nodeData, classOf[MemberStatus])
            log.debug(s"Current node data change to: ${memberStatus}")
          } else { // node was removed
            log.debug(s"${PeaConfig.zkCurrWorkerPath} was removed, will register it self again after 10 seconds")
            context.system.scheduler.scheduleOnce(10 seconds) {
              self ! TryReWatchSelf
            }
          }
        }
      })
    }
  }

  override def postStop(): Unit = {
    super.postStop()
    if (null != nodeCache) nodeCache.close()
  }
}

object WorkerActor {

  def props() = Props(new WorkerActor())

  case object WatchSelf

  case object TryReWatchSelf

  case object GetNodeStatusMessage

  case object StopEngine

  case class UpdateCodeStatus(code: Int, errMsg: String)

  case class UpdateRunningStatus(runId: String)

  case class UpdateEndStatus(code: Int, errMsg: String)

}
