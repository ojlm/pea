package asura.pea.model

case class SingleRunProgramJob(
                                worker: PeaMember,
                                request: RunProgramMessage
                              ) extends SingleJob
