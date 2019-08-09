package asura.pea.model

import asura.common.util.StringUtils

/** node data
  *
  * @param status node status
  * @param runId  report id of last job
  * @param start  start time of last job
  * @param end    end time of last job
  * @param code   code of last job
  * @param errMsg error message of last job
  */
case class MemberStatus(
                         var status: String = MemberStatus.IDLE,
                         var runId: String = StringUtils.EMPTY,
                         var start: Long = 0L,
                         var end: Long = 0L,
                         var code: Int = 0,
                         var errMsg: String = null,
                       )

object MemberStatus {

  val IDLE = "idle"
  val RUNNING = "running"
}
