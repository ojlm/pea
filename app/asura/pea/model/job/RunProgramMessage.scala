package asura.pea.model.job

import asura.common.util.StringUtils
import asura.pea.model.{LoadMessage, LoadTypes}

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
