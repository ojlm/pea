package asura.pea.actor

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.common.util.ProcessUtils
import asura.pea.PeaConfig

import scala.concurrent.{ExecutionContext, Future}

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
        ZincCompilerActor.doCompileWithErrors(msg).map(response => {
          if (response.success) {
            // TODO: get simulations set
          }
          response
        }) pipeTo sender()
      } else {
        sender() ! COMPILE_STATUS_RUNNING
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

  lazy val currentClassPath = System
    .getProperty("java.class.path")
    .split(":")
    .filter(p => {
      // filter idea jar
      !p.contains("idea_rt.jar")
    })
    .mkString(":")

  def getCmd(message: CompileMessage): String = {
    val cmd = s"java -Dfile.encoding=UTF-8 -cp ${currentClassPath} " +
      s"io.gatling.compiler.ZincCompiler " +
      s"-sf ${message.srcFolder} " +
      s"-bf ${message.outputFolder} " +
      s"${if (message.verbose) "-eso -verbose" else ""}"
    cmd
  }

  def doCompileWithErrors(message: CompileMessage): Future[CompileResponse] = {
    implicit val ec = ExecutionContext.global
    val errors = StringBuilder.newBuilder
    val futureCode = ProcessUtils.execAsync(
      getCmd(message),
      (_: String) => {},
      (stderr: String) => {
        errors.append(stderr).append("\n")
        ()
      }
    ).get
    futureCode.map(code => {
      CompileResponse(code == 0, errors.toString)
    })
  }

  def doCompile(
                 message: CompileMessage,
                 stdout: String => Unit = (_) => {},
                 stderr: String => Unit = (_) => {},
               ): Future[Int] = {
    implicit val ec = ExecutionContext.global
    ProcessUtils.execAsync(getCmd(message), stdout, stderr).get
  }

}
