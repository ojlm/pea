package asura.pea.model

import asura.common.util.StringUtils

case class RunSimulationMessage(
                                 var simulation: String,
                                 val report: Boolean = true,
                                 var simulationId: String = null,
                                 var start: Long = 0L
                               ) extends LoadMessage {

  def isValid(): Exception = {
    if (StringUtils.isEmpty(simulation)) {
      new RuntimeException("Empty simulation class")
    } else {
      null
    }
  }
}
