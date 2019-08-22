package asura.pea.gatling

import io.gatling.core.Predef.Simulation

abstract class PeaSimulation extends Simulation {

  val description: String
}
