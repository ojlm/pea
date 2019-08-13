package asura.pea.model

case class SingleHttpScenarioMessage(
                                      var name: String,
                                      var request: SingleRequest,
                                      var injections: Seq[Injection],
                                      val report: Boolean = true,
                                      var simulationId: String = null,
                                      var start: Long = 0L
                                    )
