package asura.pea.model

trait LoadJob {

  val `type`: String
  val workers: Seq[PeaMember] = null // for each worker has the same job
  val request: LoadMessage = null // for each worker has the same job
  val jobs: Seq[SingleJob] = null // each worker has itself job

  var report: Boolean = true
  var simulationId: String = null
  var start: Long = 0L
}
