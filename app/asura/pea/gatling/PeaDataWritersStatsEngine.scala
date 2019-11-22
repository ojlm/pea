package asura.pea.gatling

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import io.gatling.commons.stats.Status
import io.gatling.commons.util.Clock
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.ControllerCommand
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.session.{GroupBlock, Session}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.writer._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object PeaDataWritersStatsEngine {

  def apply(
             simulationParams: SimulationParams,
             runMessage: RunMessage,
             system: ActorSystem,
             clock: Clock,
             configuration: GatlingConfiguration
           ): PeaDataWritersStatsEngine = {
    implicit val dataWriterTimeOut: Timeout = Timeout(5 seconds)

    // include custom writers
    val dataWriters = configuration.config.getStringList(PeaConfigKeys.Writers).asScala
      .flatMap(PeaDataWriterTypes.findByName)
      .map { dw =>
        val clazz = Class.forName(dw.className).asInstanceOf[Class[Actor]]
        system.actorOf(Props(clazz, clock, configuration), clazz.getName)
      }

    val shortScenarioDescriptions = simulationParams.populationBuilders.map(pb => {
      ShortScenarioDescription(pb.scenarioBuilder.name, pb.injectionProfile.totalUserCount)
    })

    val dataWriterInitResponses = dataWriters.map(_ ? Init(simulationParams.assertions, runMessage, shortScenarioDescriptions))

    implicit val dispatcher: ExecutionContext = system.dispatcher

    val statsEngineFuture = Future.sequence(dataWriterInitResponses)
      .map(_.forall(_ == true))
      .map {
        case true => Success(new PeaDataWritersStatsEngine(dataWriters, system, clock))
        case _ => Failure(new Exception("DataWriters didn't initialize properly"))
      }

    Await.result(statsEngineFuture, 5 seconds).get
  }
}

class PeaDataWritersStatsEngine(dataWriters: Seq[ActorRef], system: ActorSystem, clock: Clock) extends StatsEngine {

  private val active = new AtomicBoolean(true)

  override def start(): Unit = {}

  override def stop(replyTo: ActorRef, exception: Option[Exception]): Unit =
    if (active.getAndSet(false)) {
      implicit val dispatcher: ExecutionContext = system.dispatcher
      implicit val dataWriterTimeOut: Timeout = Timeout(5 seconds)
      val responses = dataWriters.map(_ ? Stop)
      Future.sequence(responses).onComplete(_ => replyTo ! ControllerCommand.StatsEngineStopped)
    }

  private def dispatch(message: DataWriterMessage): Unit = if (active.get) dataWriters.foreach(_ ! message)

  override def logUser(userMessage: UserMessage): Unit = dispatch(userMessage)

  override def logResponse(
                            session: Session,
                            requestName: String,
                            startTimestamp: Long,
                            endTimestamp: Long,
                            status: Status,
                            responseCode: Option[String],
                            message: Option[String]
                          ): Unit =
    if (endTimestamp >= 0) {
      dispatch(ResponseMessage(
        session.scenario,
        session.userId,
        session.groupHierarchy,
        requestName,
        startTimestamp,
        endTimestamp,
        status,
        responseCode,
        message
      ))
    }

  override def logGroupEnd(
                            session: Session,
                            group: GroupBlock,
                            exitTimestamp: Long
                          ): Unit =
    dispatch(GroupMessage(
      session.scenario,
      session.userId,
      group.hierarchy,
      group.startTimestamp,
      exitTimestamp,
      group.cumulatedResponseTime,
      group.status
    ))

  override def logCrash(session: Session, requestName: String, error: String): Unit =
    dispatch(ErrorMessage(s"$requestName: $error ", clock.nowMillis))
}
