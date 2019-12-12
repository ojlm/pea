package pea.app.model

case class WorkersCompileRequest(
                                  workers: Seq[PeaMember],
                                  pull: Boolean,
                                )
