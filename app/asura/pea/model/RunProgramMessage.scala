package asura.pea.model

import asura.common.util.StringUtils

case class RunProgramMessage(
                              var program: String,
                              var reportStdout: Boolean = false,
                              var reportStderr: Boolean = true,
                              var simulationId: String = null,
                              var start: Long = 0L,
                            ) extends LoadMessage {

  var report: Boolean = true
  val `type`: String = LoadTypes.PROGRAM

  def isValid(): Exception = {
    if (StringUtils.isEmpty(program)) {
      new RuntimeException("Empty program")
    } else {
      null
    }
  }
}
