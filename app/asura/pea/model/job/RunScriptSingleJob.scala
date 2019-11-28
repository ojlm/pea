package asura.pea.model.job

import asura.pea.model.{PeaMember, SingleJob}

case class RunScriptSingleJob(
                               worker: PeaMember,
                               request: RunScriptMessage
                             ) extends SingleJob
