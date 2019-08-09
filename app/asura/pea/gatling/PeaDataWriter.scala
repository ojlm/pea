package asura.pea.gatling

import asura.pea.PeaConfig
import asura.pea.gatling.PeaDataWriter.{MonitorData, TotalCounters}
import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.Clock
import io.gatling.commons.util.Collections._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats.message.{End, Start}
import io.gatling.core.stats.writer._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.math.floor

class PeaDataWriter(clock: Clock, configuration: GatlingConfiguration) extends DataWriter[ConsoleData] {

  private val flushTimerName = "flushTimer"

  def onInit(init: Init): ConsoleData = {
    import init._
    val data = new ConsoleData(clock.nowMillis)
    scenarios.foreach(scenario => data.usersCounters.put(scenario.name, new UserCounters(scenario.totalUserCount)))
    setTimer(flushTimerName, Flush, configuration.config.getInt(PeaConfigKeys.WritePeriod) seconds, repeat = true)
    data
  }

  override def onFlush(data: ConsoleData): Unit = {
    import data._
    val runDuration = (clock.nowMillis - startUpTime) / 1000
    val totalWaiting = usersCounters.values.sumBy(_.waitingCount)
    val totalRunning = usersCounters.values.sumBy(_.activeCount)
    complete = (totalWaiting == 0L) && (totalRunning == 0L)
    if (null != PeaConfig.monitorActor) {
      val totalCount = usersCounters.values.sumBy(_.totalUserCount.getOrElse(0L))
      PeaConfig.monitorActor ! MonitorData(
        start = startUpTime,
        run = runDuration,
        complete = complete,
        total = TotalCounters(totalCount, totalWaiting, totalRunning),
        users = PeaDataWriter.getPeaUserCounters(usersCounters),
        requests = requestsCounters,
        global = globalRequestCounters,
        errors = errorsCounters,
      )
    }
  }

  override def onMessage(message: LoadEventMessage, data: ConsoleData): Unit = message match {
    case user: UserMessage => onUserMessage(user, data)
    case response: ResponseMessage => onResponseMessage(response, data)
    case error: ErrorMessage => onErrorMessage(error, data)
    case _ =>
  }

  private def onUserMessage(user: UserMessage, data: ConsoleData): Unit = {
    import data._
    import user._

    event match {
      case Start =>
        usersCounters.get(session.scenario) match {
          case Some(userCounters) => userCounters.userStart()
          case _ => logger.error(s"Internal error, scenario '${session.scenario}' has not been correctly initialized")
        }
      case End =>
        usersCounters.get(session.scenario) match {
          case Some(userCounters) => userCounters.userDone()
          case _ => logger.error(s"Internal error, scenario '${session.scenario}' has not been correctly initialized")
        }
    }
  }

  private def onResponseMessage(response: ResponseMessage, data: ConsoleData): Unit = {
    import data._
    import response._

    val requestPath = (groupHierarchy :+ name).mkString(" / ")
    val requestCounters = requestsCounters.getOrElseUpdate(requestPath, new RequestCounters)

    status match {
      case OK =>
        globalRequestCounters.successfulCount += 1
        requestCounters.successfulCount += 1
      case KO =>
        globalRequestCounters.failedCount += 1
        requestCounters.failedCount += 1
        val errorMessage = message.getOrElse("<no-message>")
        errorsCounters(errorMessage) = errorsCounters.getOrElse(errorMessage, 0) + 1
    }
  }

  private def onErrorMessage(error: ErrorMessage, data: ConsoleData): Unit = {
    import data._
    errorsCounters(error.message) = errorsCounters.getOrElse(error.message, 0) + 1
  }

  override def onCrash(cause: String, data: ConsoleData): Unit = {}

  override def onStop(data: ConsoleData): Unit = {
    cancelTimer(flushTimerName)
    if (!data.complete) onFlush(data)
  }
}

object PeaDataWriter {

  case class TotalCounters(
                            total: Long,
                            waiting: Long,
                            active: Long,
                          )

  case class MonitorData(
                          start: Long,
                          run: Long,
                          complete: Boolean,
                          total: TotalCounters,
                          users: mutable.Map[String, PeaUserCounters],
                          requests: mutable.Map[String, RequestCounters],
                          global: RequestCounters,
                          errors: mutable.Map[String, Int],
                        )

  case class PeaUserCounters(
                              total: Long,
                              active: Long,
                              done: Long,
                              waiting: Long,
                              percent: Int,
                            )

  def getPeaUserCounters(usersCounters: mutable.Map[String, UserCounters]): mutable.Map[String, PeaUserCounters] = {
    val counters = mutable.Map[String, PeaUserCounters]()
    usersCounters.map {
      case (scenarioName, userCounters) => userCounters.totalUserCount match {
        case Some(total) if total > userCounters.doneCount + userCounters.activeCount =>
          val donePercent = floor(100 * userCounters.doneCount.toDouble / total).toInt
          counters += (scenarioName -> PeaUserCounters(
            userCounters.totalUserCount.getOrElse(0),
            userCounters.activeCount,
            userCounters.doneCount,
            userCounters.waitingCount,
            donePercent
          ))
        case _ =>
      }
    }
    counters
  }
}
