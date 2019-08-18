package asura.pea.model

import asura.common.util.StringUtils

case class RunSimulationMessage(
                                 simulation: String,
                                 report: Boolean = true,
                                 simulationId: String = null,
                                 start: Long = 0L
                               ) extends LoadMessage {

  def isValid(): Exception = {
    if (StringUtils.isEmpty(simulation)) {
      new RuntimeException("Empty simulation class")
    } else {
      null
    }
  }
}
