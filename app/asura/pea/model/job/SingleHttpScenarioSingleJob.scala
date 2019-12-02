package asura.pea.model.job

import asura.pea.model.{PeaMember, SingleJob}

case class SingleHttpScenarioSingleJob(
                                        worker: PeaMember,
                                        load: SingleHttpScenarioMessage
                                      ) extends SingleJob
