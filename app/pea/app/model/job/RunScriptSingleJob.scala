package pea.app.model.job

import pea.app.model.{PeaMember, SingleJob}

case class RunScriptSingleJob(
                               worker: PeaMember,
                               load: RunScriptMessage
                             ) extends SingleJob
