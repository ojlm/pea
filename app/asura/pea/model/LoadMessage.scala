package asura.pea.model

trait LoadMessage {

  var simulationId: String
  var start: Long
  var report: Boolean
  // should print request and response detail
  var verbose: Boolean = false
  // load type
  val `type`: String

  def isValid(): Exception
}
