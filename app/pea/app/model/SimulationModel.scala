package pea.app.model

import pea.common.util.StringUtils

case class SimulationModel(
                            name: String,
                            protocols: Seq[String],
                            description: String = StringUtils.EMPTY
                          )
