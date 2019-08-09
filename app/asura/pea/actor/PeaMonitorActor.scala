package asura.pea.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{ActorClassifier, ActorEventBus, ManagedActorClassification}
import asura.common.actor.BaseActor
import asura.pea.actor.PeaMonitorActor.{MonitorBus, MonitorMessage, MonitorSubscriberMessage}
import asura.pea.gatling.PeaDataWriter.MonitorData

/**
  * monitor user and request counts
  */
class PeaMonitorActor extends BaseActor {

  val monitorBus = new MonitorBus(context.system)

  override def receive: Receive = {
    case MonitorSubscriberMessage(ref) =>
      monitorBus.subscribe(ref, self)
    case data: MonitorData =>
      monitorBus.publish(MonitorMessage(self, data))
    case _ =>
  }
}

object PeaMonitorActor {

  def props() = Props(new PeaMonitorActor())

  case class MonitorSubscriberMessage(ref: ActorRef)

  case class MonitorMessage(ref: ActorRef, data: MonitorData)

  class MonitorBus(val system: ActorSystem) extends ActorEventBus with ActorClassifier with ManagedActorClassification {

    override type Event = MonitorMessage

    override protected def classify(event: MonitorMessage): ActorRef = event.ref

    override protected def mapSize: Int = 1
  }

}
