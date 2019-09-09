package asura.pea.gatling

import asura.common.util.FutureUtils.RichFuture
import asura.common.util.StringUtils
import asura.pea.IDEPathHelper
import asura.pea.actor.GatlingRunnerActor
import asura.pea.actor.GatlingRunnerActor.StartMessage
import asura.pea.simulations.DubboGreetingSimulation
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.global

object RunnerSpec extends StrictLogging {

  def main(args: Array[String]): Unit = {
    run()
  }

  def run(): Unit = {
    val message = StartMessage(
      IDEPathHelper.binariesFolder.toAbsolutePath.toString,
      classOf[DubboGreetingSimulation].getName,
      true,
      IDEPathHelper.resultsFolder.toAbsolutePath.toString,
      StringUtils.EMPTY,
    )
    val code = GatlingRunnerActor.start(message)(global)
    logger.info(s"Exit: ${code}")
  }

  def report(): Unit = {
    val code = GatlingRunnerActor.generateReport(
      "runId",
      IDEPathHelper.resultsFolder.toAbsolutePath.toString,
    ).await
    logger.info(s"Exit: ${code}")
  }
}
