package pea.dubbo.action

import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.Clock
import io.gatling.core.CoreComponents
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import pea.dubbo.protocol.{DubboComponents, DubboProtocol}
import pea.dubbo.request.ReferenceConfigCache
import pea.dubbo.{DubboCheck, DubboResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success}

class DubboAction[T, V](
                         clazz: Class[T],
                         func: (T, Session) => V,
                         checks: List[DubboCheck[V]],
                         dubboComponents: DubboComponents,
                         coreComponents: CoreComponents,
                         throttled: Boolean,
                         val next: Action,
                         actionProtocol: Option[DubboProtocol] = None,
                       ) extends ExitableAction with NameGen {

  override def statsEngine: StatsEngine = coreComponents.statsEngine

  override def clock: Clock = coreComponents.clock

  override def name: String = genName("dubboRequest")

  implicit val ec = dubboComponents.executionContext
  val requestName = clazz.getName()
  val service: T = ReferenceConfigCache.get(clazz, dubboComponents.dubboProtocol, actionProtocol)

  override def execute(session: Session): Unit = {
    val startTime = System.currentTimeMillis()
    Future(func(service, session)).onComplete {
      case Success(value) =>
        val endTime = System.currentTimeMillis()
        val (newSession, error) = Check.check(DubboResponse(value), session, checks, preparedCache = null)
        error match {
          case None =>
            statsEngine.logResponse(session, requestName, startTime, endTime, OK, None, None)
            throttle(newSession)
          case Some(failure) =>
            statsEngine.logResponse(session, requestName, startTime, endTime, KO, None, Option(failure.message))
            throttle(session.markAsFailed)
        }
      case Failure(t) =>
        val endTime = System.currentTimeMillis()
        statsEngine.logResponse(session, requestName, startTime, endTime, KO, None, Option(t.getMessage))
        throttle(session.markAsFailed)
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
