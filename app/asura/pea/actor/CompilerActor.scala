package asura.pea.actor

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.StringUtils
import asura.pea.PeaConfig
import asura.pea.compiler.{CompileResponse, ScalaCompiler}
import io.gatling.app.PeaGatlingRunner

class CompilerActor extends BaseActor {

  import CompilerActor._

  implicit val ec = context.dispatcher

  var status = COMPILE_STATUS_IDLE
  var last = 0L // time last compile simulations
  var simulations: Set[String] = Set.empty

  override def receive: Receive = {
    case GetAllSimulations =>
      sender() ! Simulations(last, simulations)
    case msg: CompileMessage =>
      if (COMPILE_STATUS_IDLE == status) {
        ScalaCompiler.doCompile(msg).map(response => {
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
    case SimulationValidateMessage(simulation) =>
      sender() ! simulations.contains(simulation)
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
                             verbose: Boolean = false
                           )

  case class SimulationValidateMessage(simulation: String)

  case object GetAllSimulations

  case class Simulations(last: Long, simulations: Set[String])

}
