package asura.pea.model

import asura.pea.model.params.FinishedCallbackRequest

trait LoadJob {

  val `type`: String
  val workers: Seq[PeaMember] = null // for each worker has the same job
  val load: LoadMessage = null // for each worker has the same job
  val jobs: Seq[SingleJob] = null // each worker has itself job

  var report: Boolean = true
  var simulationId: String = null
  var start: Long = 0L
  var callback: FinishedCallbackRequest = null
}
