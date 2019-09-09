package asura.pea.dubbo.action

import java.util.concurrent.ExecutorService

import asura.pea.dubbo.DubboCheck
import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.Clock
import io.gatling.core.CoreComponents
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.check.Check
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DubboAction[A](
                      requestName: Expression[String],
                      f: (Session) => A,
                      val executor: ExecutorService,
                      val objectMapper: ObjectMapper,
                      checks: List[DubboCheck],
                      coreComponents: CoreComponents,
                      throttled: Boolean,
                      val next: Action,
                    ) extends ExitableAction with NameGen {

  implicit val ec = ExecutionContext.fromExecutor(executor)

  override def statsEngine: StatsEngine = coreComponents.statsEngine

  override def clock: Clock = coreComponents.clock

  override def name: String = genName("dubboRequest")

  override def execute(session: Session): Unit = {
    recover(session) {
      requestName(session).map { reqName =>
        val startTime = System.currentTimeMillis()
        Future(f(session)).onComplete {
          case Success(value) =>
            val endTime = System.currentTimeMillis()
            val resultJson = objectMapper.writeValueAsString(value)
            val (newSession, error) = Check.check(resultJson, session, checks)
            error match {
              case None =>
                statsEngine.logResponse(session, reqName, startTime, endTime, OK, None, None)
                throttle(newSession)
              case Some(failure) =>
                statsEngine.logResponse(session, reqName, startTime, endTime, KO, None, Option(failure.message))
                throttle(session.markAsFailed)
            }
          case Failure(t) =>
            val endTime = System.currentTimeMillis()
            statsEngine.logResponse(session, reqName, startTime, endTime, KO, None, Option(t.getMessage))
            throttle(session.markAsFailed)
        }
      }
    }
  }

  private def throttle(session: Session): Unit = {
    if (throttled) {
      coreComponents.throttler.throttle(session.scenario, () => next ! session)
    } else {
      next ! session
    }
  }
}
