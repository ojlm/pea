package pea.app.model.job

import pea.app.model.{LoadMessage, LoadTypes}
import pea.common.util.StringUtils

case class RunProgramMessage(
                              var program: String,
                              var simulationId: String = null,
                              var start: Long = 0L,
                            ) extends LoadMessage {

  val `type`: String = LoadTypes.PROGRAM
  var report: Boolean = true
  var reportStdout: Boolean = false
  var reportStderr: Boolean = true

  def isValid(): Exception = {
    if (StringUtils.isEmpty(program)) {
      new RuntimeException("Empty program")
    } else {
      null
    }
  }
}
