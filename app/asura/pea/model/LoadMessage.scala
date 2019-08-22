package asura.pea.model

trait LoadMessage {

  var simulationId: String
  var start: Long

  def isValid(): Exception
}
