/**
  * custom pea-app code from gatling.
  */
package io.gatling.app

import java.io.{File, PrintWriter}
import java.nio.file.Paths

import akka.actor.{ActorSystem, Cancellable}
import akka.pattern.ask
import asura.common.util.{LogUtils, StringUtils}
import asura.pea.PeaConfig
import asura.pea.actor.GatlingRunnerActor.{GatlingResult, PeaGatlingRunResult}
import asura.pea.gatling.{PeaDataWritersStatsEngine, PeaSimulation}
import asura.pea.model.SimulationModel
import com.typesafe.scalalogging.StrictLogging
import io.gatling.app.classloader.SimulationClassLoader
import io.gatling.commons.util.DefaultClock
import io.gatling.core.CoreComponents
import io.gatling.core.action.Exit
import io.gatling.core.config.{GatlingConfiguration, GatlingFiles, GatlingPropertiesBuilder}
import io.gatling.core.controller.inject.Injector
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.controller.{Controller, ControllerCommand}
import io.gatling.core.scenario.{Scenario, SimulationParams}
import io.gatling.core.stats.writer.RunMessage
import sbt.io.FileFilter

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

class PeaGatlingRunner(config: mutable.Map[String, _], onlyReport: Boolean = false) extends StrictLogging {

  val clock = new DefaultClock
  val configuration = GatlingConfiguration.load(config)
  val system = if (!onlyReport) {
    ActorSystem("GatlingSystem", GatlingConfiguration.loadActorSystemConfiguration())
  } else {
    null
  }
  var cancelled = false

  io.gatling.core.Predef.clock = clock
  io.gatling.core.Predef._configuration = configuration

  val cancel = new Cancellable {
    override def cancel(): Boolean = {
      cancelled = terminateActorSystem()
      cancelled
    }

    override def isCancelled: Boolean = cancelled
  }

  /**
    * simulationId and start for custom runId
    */
  def run(simulationId: String = null, nowMillis: Long = 0L)(implicit ec: ExecutionContext): PeaGatlingRunResult = {
    val selection = Selection(None, configuration)
    val simulation = selection.simulationClass.getDeclaredConstructor().newInstance()
    logger.info("Simulation instantiated")
    val simulationParams = simulation.params(configuration)
    logger.info("Simulation params built")

    simulation.executeBefore()
    logger.info("Before hooks executed")

    val runMessage = RunMessage(
      simulationParams.name,
      if (null != simulationId) simulationId else selection.simulationId,
      if (nowMillis > 0) nowMillis else clock.nowMillis,
      selection.description,
      configuration.core.version
    )

    val result = Future {
      var errMsg: String = StringUtils.EMPTY
      val runResult = try {
        val statsEngine = PeaDataWritersStatsEngine(simulationParams, runMessage, system, clock, configuration)
        val throttler = Throttler(system, simulationParams)
        val injector = Injector(system, statsEngine, clock)
        val controller = system.actorOf(Controller.props(statsEngine, injector, throttler, simulationParams, configuration), Controller.ControllerActorName)
        val exit = new Exit(injector, clock)
        val coreComponents = CoreComponents(system, controller, throttler, statsEngine, clock, exit, configuration)
        logger.info("CoreComponents instantiated")
        val scenarios = simulationParams.scenarios(coreComponents)
        start(simulationParams, scenarios, coreComponents) match {
          case Failure(t) => throw t
          case _ =>
            simulation.executeAfter()
            logger.info("After hooks executed")
            RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)
        }
      } catch {
        case t: Throwable =>
          logger.error("Run crashed", t)
          if (!cancelled) { // if the engine is stopped by hand, ignore the exception
            errMsg = t.getMessage
          }
          null
      } finally {
        terminateActorSystem()
      }
      if (null != runResult) {
        val code = new RunResultProcessor(configuration).processRunResult(runResult).code
        replaceReportLogo(runResult.runId)
        GatlingResult(code)
      } else {
        GatlingResult(-1, errMsg, cancelled)
      }
    }
    PeaGatlingRunResult(runMessage.runId, result, cancel)
  }

  def generateReport(runId: String)(implicit ec: ExecutionContext): Future[Int] = {
    Future {
      val code = new RunResultProcessor(configuration).processRunResult(RunResult(runId, true)).code
      replaceReportLogo(runId)
      code
    }
  }

  private val originLogoHref = """href="https://gatling.io""""
  private val originLogoTitle = """title="Gatling Home Page""""
  private val descHref = """href="https://gatling.io/gatling-frontline/?report""""
  private val descContent = """Get more features with Gatling FrontLine"""

  def replaceReportLogo(runId: String): Unit = {
    try {
      GatlingFiles.resultDirectory(runId)(configuration)
        .toFile
        .listFiles(new FileFilter {
          override def accept(file: File): Boolean = file.exists() && file.getName.endsWith(".html")
        })
        .foreach(file => {
          val tmpFile = new File(s"${file.getParent}/${file.getName}.tmp")
          val writer = new PrintWriter(tmpFile)
          Source.fromFile(file).getLines
            .map { line =>
              if (line.contains(originLogoHref) && StringUtils.isNotEmpty(PeaConfig.reportLogoHref)) {
                line
                  .replace(originLogoHref, s"""href="${PeaConfig.reportLogoHref}"""")
                  .replace(originLogoTitle, s"""title=${PeaConfig.reportLogoHref}"""")
              } else if (line.contains(descHref) && StringUtils.isNotEmpty(PeaConfig.reportDescHref)) {
                line
                  .replace(descHref, s"""href="${PeaConfig.reportDescHref}"""")
                  .replace(descContent, PeaConfig.reportDescContent)
              } else {
                line
              }
            }
            .foreach(writer.println)
          writer.close()
          tmpFile.renameTo(file)
        })
    } catch {
      case t: Throwable => logger.warn(LogUtils.stackTraceToString(t))
    }
  }

  private def start(simulationParams: SimulationParams, scenarios: List[Scenario], coreComponents: CoreComponents): Try[_] = {
    val timeout = Int.MaxValue.milliseconds - 10.seconds
    val start = coreComponents.clock.nowMillis
    logger.info(s"Simulation ${simulationParams.name} started...")
    logger.info("Asking Controller to start")
    val whenRunDone: Future[Try[String]] = coreComponents.controller.ask(ControllerCommand.Start(scenarios))(timeout).mapTo[Try[String]]
    val runDone = Await.result(whenRunDone, timeout)
    logger.info(s"Simulation ${simulationParams.name} completed in ${(coreComponents.clock.nowMillis - start) / 1000} seconds")
    runDone
  }

  private def terminateActorSystem(): Boolean = {
    try {
      val whenTerminated = system.terminate()
      Await.result(whenTerminated, configuration.core.shutdownTimeout milliseconds)
      true
    } catch {
      case NonFatal(e) =>
        logger.debug("Could not terminate ActorSystem", e)
        false
    }
  }
}

object PeaGatlingRunner extends StrictLogging {

  def apply(config: mutable.Map[String, _]): PeaGatlingRunner = new PeaGatlingRunner(config)

  def run(
           config: mutable.Map[String, _],
           simulationId: String = null,
           start: Long = 0L
         )(implicit ec: ExecutionContext): PeaGatlingRunResult = {
    PeaGatlingRunner(config).run(simulationId, start)
  }

  def generateReport(
                      config: mutable.Map[String, _],
                      runId: String
                    )(implicit ec: ExecutionContext): Future[Int] = {
    new PeaGatlingRunner(config, true).generateReport(runId)
  }

  // This is only for function below
  private val defaultGatlingProps = new GatlingPropertiesBuilder()
    .binariesDirectory(PeaConfig.defaultSimulationOutputFolder)
    .resourcesDirectory(PeaConfig.resourcesFolder)
    .resultsDirectory(PeaConfig.resultsFolder)
    .build

  def getSimulationClasses(binariesDirectory: String = PeaConfig.defaultSimulationOutputFolder): Seq[SimulationModel] = {
    val simulations = ArrayBuffer[SimulationModel]()
    // to instantiate PeaSimulation below
    io.gatling.core.Predef._configuration = GatlingConfiguration.load(defaultGatlingProps)
    SimulationClassLoader(Paths.get(binariesDirectory))
      .simulationClasses
      .foreach(clazz => {
        try {
          val description = if (classOf[PeaSimulation].isAssignableFrom(clazz)) {
            clazz.asInstanceOf[Class[PeaSimulation]].newInstance().description
          } else {
            StringUtils.EMPTY
          }
          simulations += SimulationModel(clazz.getName, description)
        } catch {
          case t: Throwable => logger.warn(LogUtils.stackTraceToString(t))
        }
      })
    simulations
  }
}
