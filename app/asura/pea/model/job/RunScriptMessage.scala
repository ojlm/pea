package asura.pea.model.job

import asura.pea.common.util.StringUtils
import asura.pea.model.{LoadMessage, LoadTypes}

case class RunScriptMessage(
                             var simulation: String,
                             var report: Boolean = true,
                             var simulationId: String = null,
                             var start: Long = 0L
                           ) extends LoadMessage {

  val `type`: String = LoadTypes.SCRIPT

  def isValid(): Exception = {
    if (StringUtils.isEmpty(simulation)) {
      new RuntimeException("Empty simulation class")
    } else {
      null
    }
  }
}
