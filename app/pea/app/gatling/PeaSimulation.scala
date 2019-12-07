package pea.app.gatling

import io.gatling.core.Predef.Simulation

abstract class PeaSimulation extends Simulation {

  val description: String
}
