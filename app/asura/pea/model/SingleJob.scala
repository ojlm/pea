package asura.pea.model

case class SingleJob(
                      worker: PeaMember,
                      request: RunProgramMessage
                    )
