package pea.app.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{ActorClassifier, ActorEventBus, ManagedActorClassification}
import pea.app.actor.CompilerMonitorActor.{CompilerMonitorBus, MonitorMessage, MonitorSubscriberMessage}
import pea.common.actor.BaseActor

class CompilerMonitorActor extends BaseActor {

  val monitorBus = new CompilerMonitorBus(context.system)

  override def receive: Receive = {
    case MonitorSubscriberMessage(ref) =>
      monitorBus.subscribe(ref, self)
    case data: String =>
      monitorBus.publish(MonitorMessage(self, data))
    case message: Any =>
      log.warning(s"Unknown message type ${message}")
  }
}

object CompilerMonitorActor {

  def props() = Props(new CompilerMonitorActor())

  case class MonitorSubscriberMessage(ref: ActorRef)

  case class MonitorMessage(ref: ActorRef, data: String)

  class CompilerMonitorBus(val system: ActorSystem) extends ActorEventBus with ActorClassifier with ManagedActorClassification {

    override type Event = MonitorMessage

    override protected def classify(event: MonitorMessage): ActorRef = event.ref

    override protected def mapSize: Int = 1
  }

}
