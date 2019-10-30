package asura.pea.simulation

import asura.common.util.StringUtils
import asura.pea.actor.ResponseMonitorActor
import asura.pea.gatling.PeaSimulation
import asura.pea.model.{During, Injection, SingleRequest}
import asura.pea.{PeaConfig, singleHttpScenario}
import io.gatling.core.Predef._
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.concurrent.duration._

class SingleHttpSimulation extends PeaSimulation {

  val KEY_BODY = "BODY"
  val KEY_STATUS = "STATUS"
  override val description: String = getClass.getName

  val scnName = if (StringUtils.isNotEmpty(singleHttpScenario.name)) {
    singleHttpScenario.name
  } else {
    StringUtils.notEmptyElse(singleHttpScenario.request.name, classOf[SingleHttpSimulation].getSimpleName)
  }

  val scn = scenario(scnName)
    .exec(toAction(singleHttpScenario.request))
    .exec(session => {
      if (singleHttpScenario.verbose && null != PeaConfig.responseMonitorActor && session.contains(KEY_BODY)) {
        val status = session(KEY_STATUS).as[Int]
        val response = session(KEY_BODY).as[String]
        PeaConfig.responseMonitorActor ! ResponseMonitorActor.formatResponse(status, response)
      }
      session
    })

  setUp(
    scn.inject(
      getInjectionSteps()
    ).protocols(http)
  )

  def getInjectionSteps(): Seq[OpenInjectionStep] = {
    val injections = singleHttpScenario.injections
    if (null != injections && injections.nonEmpty) {
      injections.map(injection => {
        val during = injection.during
        injection.`type` match {
          case Injection.TYPE_RAMP_USERS => rampUsers(injection.users) during (toFiniteDuration(during))
          case Injection.TYPE_HEAVISIDE_USERS => heavisideUsers(injection.users) during (toFiniteDuration(during))
          case Injection.TYPE_AT_ONCE_USERS => atOnceUsers(injection.users)
          case Injection.TYPE_CONSTANT_USERS_PER_SEC => constantUsersPerSec(injection.users) during (toFiniteDuration(during))
          case Injection.TYPE_RAMP_USERS_PER_SEC => rampUsersPerSec(injection.users) to injection.to during (toFiniteDuration(during))
        }
      })
    } else {
      Nil
    }
  }

  def toFiniteDuration(during: During): FiniteDuration = {
    during.unit match {
      case During.TIME_UNIT_MILLI => during.value millis
      case During.TIME_UNIT_SECOND => during.value seconds
      case During.TIME_UNIT_MINUTE => during.value minutes
      case During.TIME_UNIT_HOUR => during.value hours
    }
  }

  def toAction(request: SingleRequest): HttpRequestBuilder = {
    val builder = http(StringUtils.notEmptyElse(request.name, "REQUEST"))
      .httpRequest(request.method, request.url)
      .headers(request.getHeaders())
      .body(StringBody(request.getBody()))
    if (singleHttpScenario.verbose) {
      builder.check(
        bodyString.saveAs(KEY_BODY),
        status.saveAs(KEY_STATUS),
      )
    } else {
      builder
    }
  }
}
