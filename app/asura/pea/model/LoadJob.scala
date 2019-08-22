package asura.pea.model

trait LoadJob {

  val workers: Seq[PeaMember]
  val request: LoadMessage
}
