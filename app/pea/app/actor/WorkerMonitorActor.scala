package pea.app.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{ActorClassifier, ActorEventBus, ManagedActorClassification}
import pea.app.actor.WorkerMonitorActor.{MonitorMessage, MonitorSubscriberMessage, WorkerMonitorBus}
import pea.app.gatling.PeaDataWriter.MonitorData
import pea.common.actor.BaseActor

/**
  * monitor user and request counts
  */
class WorkerMonitorActor extends BaseActor {

  val monitorBus = new WorkerMonitorBus(context.system)

  override def receive: Receive = {
    case MonitorSubscriberMessage(ref) =>
      monitorBus.subscribe(ref, self)
    case data: MonitorData =>
      monitorBus.publish(MonitorMessage(self, data))
    case message: Any =>
      log.warning(s"Unknown message type ${message}")
  }
}

object WorkerMonitorActor {

  def props() = Props(new WorkerMonitorActor())

  case class MonitorSubscriberMessage(ref: ActorRef)

  case class MonitorMessage(ref: ActorRef, data: MonitorData)

  class WorkerMonitorBus(val system: ActorSystem) extends ActorEventBus with ActorClassifier with ManagedActorClassification {

    override type Event = MonitorMessage

    override protected def classify(event: MonitorMessage): ActorRef = event.ref

    override protected def mapSize: Int = 1
  }

}
