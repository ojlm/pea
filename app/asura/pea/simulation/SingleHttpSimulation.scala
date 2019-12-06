package asura.pea.simulation

import asura.pea.actor.ResponseMonitorActor
import asura.pea.common.util.StringUtils
import asura.pea.gatling.PeaSimulation
import asura.pea.model.params.{AssertionItem, DurationParam, FeederParam, ThrottleStep => ThrottleStepParam}
import asura.pea.model.{Injection, SingleRequest}
import asura.pea.{PeaConfig, singleHttpScenario}
import io.gatling.core.Predef._
import io.gatling.core.controller.inject.open.OpenInjectionStep
import io.gatling.core.controller.throttle.ThrottleStep
import io.gatling.core.feeder.FeederBuilder
import io.gatling.http.Predef._
import io.gatling.http.check.HttpCheck
import io.gatling.http.request.builder.HttpRequestBuilder

import scala.collection.mutable.ArrayBuffer
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

  val _chain = {
    exec(toRequestBuilder(singleHttpScenario.request))
      .exec(session => {
        if (singleHttpScenario.verbose && null != PeaConfig.responseMonitorActor && session.contains(KEY_BODY)) {
          val status = session(KEY_STATUS).as[Int]
          val response = session(KEY_BODY).as[String]
          PeaConfig.responseMonitorActor ! ResponseMonitorActor.formatResponse(status, response)
        }
        session
      })
  }
  val chain = if (hasLoops()) {
    val loopParam = singleHttpScenario.loop
    if (loopParam.forever) {
      forever(_chain)
    } else {
      repeat(loopParam.repeat)(_chain)
    }
  } else {
    _chain
  }
  val feeder = getFeeder()

  val scn = if (null != feeder) {
    scenario(scnName).feed(feeder).exec(chain)
  } else {
    scenario(scnName).exec(chain)
  }

  if (hasMaxDuration()) {
    setUp(
      scn.inject(
        getInjectionSteps()
      ).protocols(http.disableCaching)
    ).throttle(getThrottleSteps()).maxDuration(toFiniteDuration(singleHttpScenario.maxDuration))
  } else {
    setUp(
      scn.inject(
        getInjectionSteps()
      ).protocols(http.disableCaching)
    ).throttle(getThrottleSteps())
  }

  def hasMaxDuration(): Boolean = {
    val maxDuration = singleHttpScenario.maxDuration
    null != maxDuration && StringUtils.isNotEmpty(maxDuration.unit) && maxDuration.value > 0
  }

  def hasLoops(): Boolean = {
    val loopParam = singleHttpScenario.loop
    null != loopParam && (loopParam.forever || loopParam.repeat > 0)
  }

  def getFeeder(): FeederBuilder = {
    val feederParam = singleHttpScenario.feeder
    if (null != feederParam && StringUtils.isNotEmpty(feederParam.path)) {
      feederParam.`type` match {
        case FeederParam.TYPE_CSV => csv(feederParam.path).batch(200).circular
        case FeederParam.TYPE_JSON => jsonFile(feederParam.path).circular
        case _ => null
      }
    } else {
      null
    }
  }

  def getThrottleSteps(): Seq[ThrottleStep] = {
    val throttle = singleHttpScenario.throttle
    if (null != throttle && null != throttle.steps && throttle.steps.nonEmpty) {
      throttle.steps.map(step => {
        step.`type` match {
          case ThrottleStepParam.TYPE_REACH => reachRps(step.rps) in toFiniteDuration(step.duration)
          case ThrottleStepParam.TYPE_HOLD => holdFor(toFiniteDuration(step.duration))
          case ThrottleStepParam.TYPE_JUMP => jumpToRps(step.rps)
        }
      })
    } else {
      Nil
    }
  }

  def getInjectionSteps(): Seq[OpenInjectionStep] = {
    val injections = singleHttpScenario.injections
    if (null != injections && injections.nonEmpty) {
      injections.map(injection => {
        val duration = injection.duration
        injection.`type` match {
          case Injection.TYPE_RAMP_USERS => rampUsers(injection.users) during (toFiniteDuration(duration))
          case Injection.TYPE_HEAVISIDE_USERS => heavisideUsers(injection.users) during (toFiniteDuration(duration))
          case Injection.TYPE_AT_ONCE_USERS => atOnceUsers(injection.users)
          case Injection.TYPE_CONSTANT_USERS_PER_SEC => constantUsersPerSec(injection.users) during (toFiniteDuration(duration))
          case Injection.TYPE_RAMP_USERS_PER_SEC => rampUsersPerSec(injection.users) to injection.to during (toFiniteDuration(duration))
        }
      })
    } else {
      Nil
    }
  }

  def getChecks(): Seq[HttpCheck] = {
    val checks = ArrayBuffer[HttpCheck]()
    val assertions = singleHttpScenario.assertions
    if (null != assertions) {
      if (null != assertions.status && null != assertions.status.list) {
        assertions.status.list.foreach(item => {
          item.op match {
            case AssertionItem.TYPE_EQ => checks += status.is(item.expect.asInstanceOf[Int])
            case _ =>
          }
        })
      }
      if (null != assertions.header && null != assertions.header.list) {
        assertions.header.list.foreach(item => {
          item.op match {
            case AssertionItem.TYPE_EQ => checks += header(item.path).is(item.expect.asInstanceOf[String])
            case _ =>
          }
        })
      }
      if (null != assertions.body && null != assertions.body.list) {
        assertions.body.list.foreach(item => {
          item.op match {
            case AssertionItem.TYPE_JSONPATH => checks += jsonPath(item.path).is(item.expect.asInstanceOf[String])
            case _ =>
          }
        })
      }
    }
    checks
  }

  def toFiniteDuration(duration: DurationParam): FiniteDuration = {
    duration.unit match {
      case DurationParam.TIME_UNIT_MILLI => duration.value millis
      case DurationParam.TIME_UNIT_SECOND => duration.value seconds
      case DurationParam.TIME_UNIT_MINUTE => duration.value minutes
      case DurationParam.TIME_UNIT_HOUR => duration.value hours
    }
  }

  def toRequestBuilder(request: SingleRequest): HttpRequestBuilder = {
    val builder = http(StringUtils.notEmptyElse(request.name, request.url))
      .httpRequest(request.method, request.url)
      .headers(request.getHeaders())
      .body(StringBody(request.getBody()))
      .check(getChecks(): _*)
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
