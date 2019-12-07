package pea.app.model.job

import pea.app.model.{LoadMessage, LoadTypes}
import pea.common.util.StringUtils

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
