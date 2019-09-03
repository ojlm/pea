package asura.pea.actor

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.StringUtils
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
    case msg: CompileMessage =>
      if (COMPILE_STATUS_IDLE == status) {
        val pullFutureCode = if (msg.pull) CompilerActor.runGitPull() else Future.successful(0)
        pullFutureCode.flatMap(code => {
          if (0 == code) {
            ScalaCompiler.doCompile(msg)
          } else {
            Future.successful(CompileResponse(false, "Run git pull fail."))
          }
        }).map(response => {
          if (response.success) {
            last = System.currentTimeMillis()
            val f = StringUtils.notEmptyElse(msg.outputFolder, PeaConfig.defaultSimulationOutputFolder)
            this.simulations = PeaGatlingRunner.getSimulationClasses(f)
          }
          response
        }) pipeTo sender()
      } else {
        sender() ! CompileResponse(false, "Compiler is running.")
      }
    case msg: AsyncCompileMessage =>
      sender() ! true
      val pullFutureCode = if (msg.pull) CompilerActor.runGitPull() else Future.successful(0)
      pullFutureCode.map(code => if (0 == code) ScalaCompiler.doCompile(CompileMessage()))
    case SimulationValidateMessage(simulation) =>
      sender() ! simulations.find(_.name.equals(simulation)).nonEmpty
    case _ =>
  }
}

object CompilerActor {

  def props() = Props(new CompilerActor())

  val COMPILE_STATUS_IDLE = 0
  val COMPILE_STATUS_RUNNING = 1

  case class CompileMessage(
                             srcFolder: String = PeaConfig.defaultSimulationSourceFolder,
                             outputFolder: String = PeaConfig.defaultSimulationOutputFolder,
                             verbose: Boolean = false,
                             pull: Boolean = false, // run git pull before compile
                           )

  // respond immediately
  case class AsyncCompileMessage(
                                  pull: Boolean = false, // run git pull before compile
                                )

  case class SimulationValidateMessage(simulation: String)

  case object GetAllSimulations

  case class Simulations(last: Long, simulations: Seq[SimulationModel])

  def runGitPull(): Future[Int] = {
    implicit val ec = ExecutionContext.global
    // TODO
    // ProcessUtils.execAsync()
    Future.successful(0)
  }
}
