package asura.pea.gatling

import asura.common.util.FutureUtils.RichFuture
import asura.common.util.StringUtils
import asura.pea.actor.GatlingRunnerActor
import asura.pea.actor.GatlingRunnerActor.StartMessage
import asura.pea.simulations.GrpcHelloSimulation
import asura.pea.{IDEPathHelper, PeaConfig}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.global

object RunnerSpec extends StrictLogging {

  PeaConfig.defaultSimulationOutputFolder = IDEPathHelper.binariesFolder.toAbsolutePath.toString

  def main(args: Array[String]): Unit = {
    run()
  }

  def run(): Unit = {
    val message = StartMessage(
      IDEPathHelper.binariesFolder.toAbsolutePath.toString,
      classOf[GrpcHelloSimulation].getName,
      true,
      IDEPathHelper.resultsFolder.toAbsolutePath.toString,
      StringUtils.EMPTY,
    )
    val result = GatlingRunnerActor.start(message)(global).result.await
    logger.info(s"Exit: ${result}")
  }

  def report(): Unit = {
    val result = GatlingRunnerActor.generateReport(
      "runId",
      IDEPathHelper.resultsFolder.toAbsolutePath.toString,
    ).await
    logger.info(s"Exit: ${result}")
  }
}
