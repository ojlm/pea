package pea.app.model.job

import pea.app.model.{PeaMember, SingleJob}

case class SingleHttpScenarioSingleJob(
                                        worker: PeaMember,
                                        load: SingleHttpScenarioMessage
                                      ) extends SingleJob
