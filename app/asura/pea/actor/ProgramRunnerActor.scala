package asura.pea.actor

import akka.actor.{Cancellable, Props}
import asura.common.actor.BaseActor
import asura.common.util.ProcessUtils.AsyncIntResult
import asura.common.util.{ProcessUtils, StringUtils, XtermUtils}
import asura.pea.PeaConfig
import asura.pea.actor.ProgramRunnerActor.ProgramResult
import asura.pea.model.RunProgramMessage

import scala.concurrent.{ExecutionContext, Future}

class ProgramRunnerActor extends BaseActor {

  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case RunProgramMessage(program, _, simulationId, start) =>
      sender() ! runCmd(program, simulationId, start)
  }

  def runCmd(cmd: String, simulationId: String, start: Long): ProgramResult = {
    val result = ProgramRunnerActor.run(cmd)
    val cancellable = new Cancellable {
      override def cancel(): Boolean = {
        result.cancel()
        true
      }

      override def isCancelled: Boolean = false
    }
    ProgramResult(s"${StringUtils.notEmptyElse(simulationId, PeaConfig.hostname)}-${start}", result.get, cancellable)
  }
}

object ProgramRunnerActor {

  def props() = Props(new ProgramRunnerActor())

  def run(cmd: String): AsyncIntResult = {
    implicit val ec = ExecutionContext.global
    ProcessUtils.execAsync(
      cmd,
      (stdout: String) => if (null != PeaConfig.responseMonitorActor) {
        PeaConfig.responseMonitorActor ! s"${XtermUtils.greenWrap("[info ]")} ${stdout}"
      },
      (stderr: String) => if (null != PeaConfig.responseMonitorActor) {
        PeaConfig.responseMonitorActor ! s"${XtermUtils.redWrap("[error]")} ${stderr}"
      },
      None
    )
  }

  case class ProgramResult(runId: String, result: Future[Int], cancel: Cancellable)

}
