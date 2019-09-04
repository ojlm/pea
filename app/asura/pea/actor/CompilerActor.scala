package asura.pea.actor

import java.io.File

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.{ProcessUtils, StringUtils, XtermUtils}
import asura.pea.PeaConfig
import asura.pea.compiler.{CompileResponse, ScalaCompiler}
import asura.pea.model.SimulationModel
import io.gatling.app.PeaGatlingRunner

import scala.concurrent.{ExecutionContext, Future}

class CompilerActor extends BaseActor {

  import CompilerActor._

  implicit val ec = context.dispatcher

  var status = COMPILE_STATUS_IDLE
  var last = 0L // time last compile simulations
  var simulations: Seq[SimulationModel] = Nil

  override def receive: Receive = {
    case GetAllSimulations =>
      sender() ! Simulations(last, simulations)
    case msg: SyncCompileMessage =>
      if (COMPILE_STATUS_IDLE == status) {
        val pullFutureCode = if (msg.pull) CompilerActor.runGitPull() else Future.successful(0)
        pullFutureCode.flatMap(code => {
          if (0 == code) {
            status = COMPILE_STATUS_RUNNING
            ScalaCompiler.doCompile(msg)
          } else {
            Future.successful(CompileResponse(false, "Run git pull fail."))
          }
        }).map(response => {
          status = COMPILE_STATUS_IDLE
          if (response.success) {
            last = System.currentTimeMillis()
            val f = StringUtils.notEmptyElse(msg.outputFolder, PeaConfig.defaultSimulationOutputFolder)
            this.simulations = PeaGatlingRunner.getSimulationClasses(f)
          }
          response
        }) pipeTo sender()
      } else {
        sender() ! CompileResponse(true, "Compiler is running.")
      }
    case msg: AsyncCompileMessage =>
      sender() ! true
      val pullFutureCode = if (msg.pull) CompilerActor.runGitPull() else Future.successful(0)
      pullFutureCode.map(code => {
        if (0 == code && COMPILE_STATUS_IDLE == status) {
          status = COMPILE_STATUS_RUNNING
          ScalaCompiler.doCompile(SyncCompileMessage()).map(_ => {
            last = System.currentTimeMillis()
            status = COMPILE_STATUS_IDLE
          })
        }
      })
    case SimulationValidateMessage(simulation) =>
      sender() ! simulations.find(_.name.equals(simulation)).nonEmpty
    case _ =>
  }
}

object CompilerActor {

  def props() = Props(new CompilerActor())

  val COMPILE_STATUS_IDLE = 0
  val COMPILE_STATUS_RUNNING = 1

  trait CompileMessage

  case class SyncCompileMessage(
                                 srcFolder: String = PeaConfig.defaultSimulationSourceFolder,
                                 outputFolder: String = PeaConfig.defaultSimulationOutputFolder,
                                 verbose: Boolean = false,
                                 pull: Boolean = false, // run git pull before compile
                               ) extends CompileMessage

  // respond immediately
  case class AsyncCompileMessage(
                                  pull: Boolean = false, // run git pull before compile
                                ) extends CompileMessage

  case class SimulationValidateMessage(simulation: String)

  case object GetAllSimulations

  case class Simulations(
                          last: Long,
                          simulations: Seq[SimulationModel],
                          editorBaseUrl: String = PeaConfig.webSimulationEditorBaseUrl,
                        )

  def runGitPull(): Future[Int] = {
    implicit val ec = ExecutionContext.global
    ProcessUtils.execAsync(
      "git pull",
      (stdout: String) => if (null != PeaConfig.compilerMonitorActor) {
        PeaConfig.compilerMonitorActor ! s"${XtermUtils.greenWrap("[info ]")} ${stdout}"
      },
      (stderr: String) => if (null != PeaConfig.compilerMonitorActor) {
        PeaConfig.compilerMonitorActor ! s"${XtermUtils.redWrap("[error]")} ${stderr}"
      },
      Some(new File(PeaConfig.defaultSimulationSourceFolder))
    ).get
  }
}
