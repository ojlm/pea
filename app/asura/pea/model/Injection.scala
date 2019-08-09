package asura.pea.model

// https://gatling.io/docs/current/general/simulation_setup
case class Injection(
                      var `type`: String,
                      var users: Int,
                      var to: Int = 0,
                      var during: During = null,
                    )

object Injection {

  val TYPE_RAMP_USERS = "rampUsers"
  val TYPE_HEAVISIDE_USERS = "heavisideUsers"
  val TYPE_AT_ONCE_USERS = "atOnceUsers"
  val TYPE_CONSTANT_USERS_PER_SEC = "constantUsersPerSec"
  val TYPE_RAMP_USERS_PER_SEC = "rampUsersPerSec"
}
