package pea.app.model

import pea.app.model.ReporterJobStatus.JobWorkerStatus
import pea.common.util.StringUtils

import scala.collection.mutable

case class ReporterJobStatus(
                              var status: String = MemberStatus.REPORTER_FINISHED,
                              var runId: String = StringUtils.EMPTY,
                              var start: Long = 0L,
                              var end: Long = 0L,
                              var workers: mutable.Map[String, JobWorkerStatus] = mutable.Map.empty,
                              var load: Any = null, // any for jackson
                            )

object ReporterJobStatus {

  case class JobWorkerStatus(
                              status: String = MemberStatus.WORKER_IDLE,
                              errMsg: String = null,
                            )

}
