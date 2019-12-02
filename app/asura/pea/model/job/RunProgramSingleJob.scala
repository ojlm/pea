package asura.pea.model.job

import asura.pea.model.{PeaMember, SingleJob}

case class RunProgramSingleJob(
                                worker: PeaMember,
                                load: RunProgramMessage
                              ) extends SingleJob
