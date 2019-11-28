package asura.pea.model.job

import asura.pea.model.{PeaMember, SingleJob}

case class SingleHttpScenarioSingleJob(
                                        worker: PeaMember,
                                        request: SingleHttpScenarioMessage
                                      ) extends SingleJob
