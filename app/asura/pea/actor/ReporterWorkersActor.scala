package asura.pea.actor

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.common.model.ApiRes
import asura.pea.model.{LoadMessage, PeaMember, SingleHttpScenarioMessage}
import asura.pea.service.PeaService

// FIXME: assume that all workers are still idle
class ReporterWorkersActor(workers: Seq[PeaMember]) extends BaseActor {

  implicit val ec = context.dispatcher
  val runId = self.path.name

  override def receive: Receive = {
    case msg: SingleHttpScenarioMessage =>
      watchNodeAndSendLoad(msg)
    case _ =>
  }

  override def postStop(): Unit = {
    // TODO: rmr zk nodes
  }

  def watchNodeAndSendLoad(load: LoadMessage): Unit = {
    // TODO: register job node
    workers.foreach(worker => load match {
      case msg: SingleHttpScenarioMessage =>
        // TODO: watch first, then send load
        PeaService.sendSingleHttpScenario(worker, msg).map(_ => watchWorkerNode(_, worker))
      case _ =>
        context stop self
    })
  }

  def watchWorkerNode(res: ApiRes, worker: PeaMember): Unit = {

  }
}

object ReporterWorkersActor {

  def props(workers: Seq[PeaMember]) = Props(new ReporterWorkersActor(workers))
}
