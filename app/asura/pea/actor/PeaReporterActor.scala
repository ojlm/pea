package asura.pea.actor

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.pea.actor.PeaReporterActor.SingleHttpScenarioJob
import asura.pea.actor.PeaWorkerActor.SingleHttpScenarioMessage
import asura.pea.model.PeaMember

class PeaReporterActor extends BaseActor {

  override def receive: Receive = {
    case SingleHttpScenarioJob(workers, request) =>
    // TODO
    case _ =>
  }
}

object PeaReporterActor {

  def props() = Props(new PeaReporterActor())

  case class SingleHttpScenarioJob(workers: Seq[PeaMember], request: SingleHttpScenarioMessage)

}
