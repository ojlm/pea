package pea.app.model.job

import pea.app.model.{PeaMember, SingleJob}

case class RunProgramSingleJob(
                                worker: PeaMember,
                                load: RunProgramMessage
                              ) extends SingleJob
