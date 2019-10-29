package asura.pea.model

trait LoadMessage {

  var simulationId: String
  var start: Long
  // should print request and response detail
  var verbose: Boolean = false

  def isValid(): Exception
}
