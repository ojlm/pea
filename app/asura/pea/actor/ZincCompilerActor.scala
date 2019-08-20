package asura.pea.actor

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.StringUtils
import asura.pea.PeaConfig
import asura.pea.compiler.ZincCompiler
import io.gatling.app.PeaGatlingRunner

class ZincCompilerActor extends BaseActor {

  import ZincCompilerActor._

  implicit val ec = context.dispatcher

  var status = COMPILE_STATUS_IDLE
  var last = 0L // time last compile simulations
  var simulations: Set[String] = Set.empty

  override def receive: Receive = {
    case GetAllSimulations =>
      sender() ! Simulations(last, simulations)
    case msg: CompileMessage =>
      if (COMPILE_STATUS_IDLE == status) {
        ZincCompiler.doGatlingCompileWithErrors(msg).map(response => {
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

object ZincCompilerActor {

  def props() = Props(new ZincCompilerActor())

  val COMPILE_STATUS_IDLE = 0
  val COMPILE_STATUS_RUNNING = 1

  case class CompileMessage(
                             srcFolder: String = PeaConfig.defaultSimulationSourceFolder,
                             outputFolder: String = PeaConfig.defaultSimulationSourceFolder,
                             verbose: Boolean = false
                           )

  case class CompileResponse(success: Boolean, errMsg: String)

  case class SimulationValidateMessage(simulation: String)

  case object GetAllSimulations

  case class Simulations(last: Long, simulations: Set[String])

}
