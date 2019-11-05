package asura.pea.actor

import java.io.{File, PrintWriter}
import java.nio.file.Files

import akka.actor.{Cancellable, Props}
import asura.common.actor.BaseActor
import asura.common.util.ProcessUtils.AsyncIntResult
import asura.common.util.{LogUtils, ProcessUtils, StringUtils, XtermUtils}
import asura.pea.PeaConfig
import asura.pea.actor.ProgramRunnerActor.{ProgramResult, logger}
import asura.pea.model.RunProgramMessage
import asura.pea.service.PeaService
import asura.pea.util.SimulationLogUtils
import com.typesafe.scalalogging.Logger

import scala.concurrent.{ExecutionContext, Future}

class ProgramRunnerActor extends BaseActor {

  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case msg: RunProgramMessage =>
      sender() ! runCmd(msg)
  }

  def runCmd(msg: RunProgramMessage): ProgramResult = {
    val runId = if (StringUtils.isEmpty(msg.simulationId) && msg.start < 1) {
      PeaService.generateRunId(PeaConfig.hostname, System.currentTimeMillis())
    } else {
      PeaService.generateRunId(msg.simulationId, msg.start)
    }
    val writer: PrintWriter = if (msg.report) {
      try {
        val file = new File(SimulationLogUtils.simulationLogFile(runId))
        val parent = file.getParentFile
        if (!parent.exists()) Files.createDirectory(parent.toPath)
        new PrintWriter(file)
      } catch {
        case t: Throwable =>
          logger.warn(LogUtils.stackTraceToString(t))
          null
      }
    } else {
      null
    }
    val result = ProgramRunnerActor.run(msg.program, writer, msg.verbose, msg.reportStdout, msg.reportStderr)
    val cancellable = new Cancellable {
      override def cancel(): Boolean = {
        result.cancel()
        if (null != writer) writer.close()
        true
      }

      override def isCancelled: Boolean = false
    }
    val futureCode = result.get.map(code => {
      if (null != writer) writer.close()
      code
    }).recover {
      case t: Throwable =>
        if (null != writer) writer.close()
        throw t
    }
    ProgramResult(runId, futureCode, cancellable)
  }
}

object ProgramRunnerActor {

  val logger = Logger(getClass.getName)

  def props() = Props(new ProgramRunnerActor())

  def run(
           cmd: String,
           writer: PrintWriter,
           verbose: Boolean,
           reportStdout: Boolean,
           reportStderr: Boolean,
         ): AsyncIntResult = {
    implicit val ec = ExecutionContext.global
    ProcessUtils.execAsync(
      cmd,
      (stdout: String) => {
        if (null != PeaConfig.responseMonitorActor && verbose) {
          PeaConfig.responseMonitorActor ! s"${XtermUtils.greenWrap("[info ]")} ${stdout}"
        }
        if (null != writer && reportStdout) writer.println(stdout)
      },
      (stderr: String) => {
        if (null != PeaConfig.responseMonitorActor && verbose) {
          PeaConfig.responseMonitorActor ! s"${XtermUtils.redWrap("[error]")} ${stderr}"
        }
        if (null != writer && reportStderr) writer.println(stderr)
      },
      None
    )
  }

  case class ProgramResult(runId: String, result: Future[Int], cancel: Cancellable)

}
