package asura.pea.actor

import akka.actor.{Cancellable, Props}
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.pea.PeaConfig
import asura.pea.actor.GatlingRunnerActor.{GenerateReport, StartMessage}
import asura.pea.actor.PeaWorkerActor.SingleHttpScenarioMessage
import asura.pea.simulation.SingleHttpSimulation
import io.gatling.app.PeaGatlingRunner
import io.gatling.core.config.GatlingPropertiesBuilder

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class GatlingRunnerActor extends BaseActor {

  implicit val ec = context.dispatcher
  val innerClassPath = getClass.getResource("/").getPath
  val singleHttpSimulationRef = classOf[SingleHttpSimulation].getCanonicalName

  override def receive: Receive = {
    case msg: StartMessage =>
      sender() ! GatlingRunnerActor.start(msg)
    case SingleHttpScenarioMessage(_, _, _, report) =>
      sender() ! GatlingRunnerActor.start(StartMessage(innerClassPath, singleHttpSimulationRef, report))
    case GenerateReport(runId) =>
      GatlingRunnerActor.generateReport(runId) pipeTo sender()
  }
}

object GatlingRunnerActor {

  case class StartMessage(
                           binariesFolder: String,
                           simulationClass: String,
                           report: Boolean = true,
                           resultsFolder: String = PeaConfig.resultsFolder
                         ) {

    def toGatlingPropertiesMap: mutable.Map[String, _] = {
      val props = new GatlingPropertiesBuilder()
        .binariesDirectory(binariesFolder)
        .resultsDirectory(resultsFolder)
        .simulationClass(simulationClass)
      if (!report) props.noReports()
      props.build
    }
  }

  def props() = Props(new GatlingRunnerActor())

  def start(message: StartMessage)(implicit ec: ExecutionContext): PeaGatlingRunResult = {
    PeaGatlingRunner.run(message.toGatlingPropertiesMap)
  }

  def generateReport(runId: String, resultsFolder: String = PeaConfig.resultsFolder): Future[Int] = {
    val props = new GatlingPropertiesBuilder()
      .resultsDirectory(resultsFolder)
      .build
    PeaGatlingRunner.generateReport(props, runId)(scala.concurrent.ExecutionContext.global)
  }

  case class PeaGatlingRunResult(runId: String, result: Future[GatlingResult], cancel: Cancellable)

  case class GatlingResult(code: Int, errMsg: String = null, isByCanceled: Boolean = false)

  case class GenerateReport(runId: String)

}
