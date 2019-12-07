package pea.app.model

trait SingleJob {
  val worker: PeaMember
  val load: LoadMessage
}
