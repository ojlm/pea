package pea.app.model

import pea.app.model.params.DurationParam

// https://gatling.io/docs/current/general/simulation_setup
case class Injection(
                      var `type`: String,
                      var users: Int,
                      var from: Int = 0,
                      var to: Int = 0,
                      var duration: DurationParam = null,
                      var times: Int = 0,
                      var eachLevelLasting: DurationParam = null,
                      var separatedByRampsLasting: DurationParam = null,
                    )

object Injection {

  // Open Model
  val TYPE_NOTHING_FOR = "nothingFor"
  val TYPE_AT_ONCE_USERS = "atOnceUsers"
  val TYPE_RAMP_USERS = "rampUsers"
  val TYPE_CONSTANT_USERS_PER_SEC = "constantUsersPerSec"
  val TYPE_RAMP_USERS_PER_SEC = "rampUsersPerSec"
  val TYPE_HEAVISIDE_USERS = "heavisideUsers"
  val TYPE_INCREMENT_USERS_PER_SEC = "incrementUsersPerSec" // meta

  // Close Model
  val TYPE_CONSTANT_CONCURRENT_USERS = "constantConcurrentUsers"
  val TYPE_RAMP_CONCURRENT_USERS = "rampConcurrentUsers"
  val TYPE_INCREMENT_CONCURRENT_USERS = "incrementConcurrentUsers" // meta
}
