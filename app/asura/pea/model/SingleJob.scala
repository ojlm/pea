package asura.pea.model

trait SingleJob {
  val worker: PeaMember
  val request: LoadMessage
}
