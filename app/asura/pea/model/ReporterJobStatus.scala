package asura.pea.model

import asura.common.util.StringUtils
import asura.pea.model.ReporterJobStatus.JobWorkerStatus

import scala.collection.mutable

case class ReporterJobStatus(
                              var status: String = MemberStatus.RUNNING,
                              var runId: String = StringUtils.EMPTY,
                              var start: Long = 0L,
                              var end: Long = 0L,
                              var workers: mutable.Map[String, JobWorkerStatus] = mutable.Map.empty
                            )

object ReporterJobStatus {

  // the worker status can only be one of `running`, `ill` and `finished`.
  case class JobWorkerStatus(
                              status: String = MemberStatus.RUNNING,
                              errMsg: String = null,
                            )

}
