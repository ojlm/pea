package asura.pea.model

import asura.common.util.StringUtils

case class RunProgramMessage(
                              var program: String,
                              var report: Boolean = false,
                              var simulationId: String = null,
                              var start: Long = 0L
                            ) extends LoadMessage {

  val `type`: String = LoadTypes.PROGRAM

  def isValid(): Exception = {
    if (StringUtils.isEmpty(program)) {
      new RuntimeException("Empty program")
    } else {
      null
    }
  }
}
