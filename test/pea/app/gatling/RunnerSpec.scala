package pea.app.gatling

import com.typesafe.scalalogging.StrictLogging
import pea.app.actor.GatlingRunnerActor
import pea.app.actor.GatlingRunnerActor.StartMessage
import pea.app.simulations.GrpcHelloSimulation
import pea.app.{IDEPathHelper, PeaConfig}
import pea.common.util.FutureUtils.RichFuture
import pea.common.util.StringUtils

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
