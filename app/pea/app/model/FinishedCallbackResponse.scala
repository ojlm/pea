package pea.app.model

import pea.app.gatling.PeaRequestStatistics

case class FinishedCallbackResponse(
                                     runId: String,
                                     start: Long,
                                     end: Long,
                                     code: Int,
                                     errMsg: String = null,
                                     statistics: PeaRequestStatistics = null,
                                     ext: Any = null,
                                   )
