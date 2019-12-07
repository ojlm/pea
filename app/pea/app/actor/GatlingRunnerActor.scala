package pea.app.actor

import akka.actor.{Cancellable, Props}
import akka.pattern.pipe
import io.gatling.app.PeaGatlingRunner
import io.gatling.core.config.GatlingPropertiesBuilder
import pea.app.PeaConfig
import pea.app.actor.GatlingRunnerActor.{GenerateReport, StartMessage}
import pea.app.gatling.PeaRequestStatistics
import pea.app.model.job.{RunScriptMessage, SingleHttpScenarioMessage}
import pea.app.simulation.SingleHttpSimulation
import pea.common.actor.BaseActor

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class GatlingRunnerActor extends BaseActor {

  implicit val ec = context.dispatcher
  val innerClassPath = getClass.getResource("/").getPath
  val singleHttpSimulationRef = classOf[SingleHttpSimulation].getCanonicalName

  override def receive: Receive = {
    case msg: StartMessage =>
      sender() ! GatlingRunnerActor.start(msg)
    case msg: SingleHttpScenarioMessage =>
      sender() ! GatlingRunnerActor.start(
        StartMessage(innerClassPath, singleHttpSimulationRef, msg.report),
        msg.simulationId,
        msg.start
      )
    case msg: RunScriptMessage =>
      sender() ! GatlingRunnerActor.start(
        StartMessage(PeaConfig.defaultSimulationOutputFolder, msg.simulation, msg.report),
        msg.simulationId,
        msg.start
      )
    case GenerateReport(runId) =>
      GatlingRunnerActor.generateReport(runId) pipeTo sender()
  }
}

object GatlingRunnerActor {

  case class StartMessage(
                           binariesFolder: String,
                           simulationClass: String,
                           report: Boolean = true,
                           resultsFolder: String = PeaConfig.resultsFolder,
                           resourcesFolder: String = PeaConfig.resourcesFolder,
                         ) {

    def toGatlingPropertiesMap: mutable.Map[String, _] = {
      val props = new GatlingPropertiesBuilder()
        .binariesDirectory(binariesFolder)
        .resourcesDirectory(resourcesFolder)
        .resultsDirectory(resultsFolder)
        .simulationClass(simulationClass)
      if (!report) props.noReports()
      props.build
    }
  }

  def props() = Props(new GatlingRunnerActor())

  def start(
             message: StartMessage,
             simulationId: String = null,
             start: Long = 0L
           )(implicit ec: ExecutionContext): PeaGatlingRunResult = {
    PeaGatlingRunner.run(message.toGatlingPropertiesMap, simulationId, start)
  }

  def generateReport(runId: String, resultsFolder: String = PeaConfig.resultsFolder): Future[GatlingReportResult] = {
    val props = new GatlingPropertiesBuilder()
      .resultsDirectory(resultsFolder)
      .build
    PeaGatlingRunner.generateReport(props, runId)(scala.concurrent.ExecutionContext.global)
  }

  case class PeaGatlingRunResult(
                                  runId: String,
                                  result: Future[GatlingResult],
                                  cancel: Cancellable,
                                  error: Throwable = null,
                                )

  case class GatlingResult(
                            code: Int,
                            errMsg: String = null,
                            isByCanceled: Boolean = false,
                            statistics: PeaRequestStatistics = null,
                          )

  case class GatlingReportResult(
                                  code: Int,
                                  errMsg: String = null,
                                  statistics: PeaRequestStatistics = null,
                                )

  case class GenerateReport(runId: String)

}
