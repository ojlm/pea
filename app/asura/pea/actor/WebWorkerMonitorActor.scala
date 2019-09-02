package asura.pea.actor

import akka.actor.{ActorRef, Props}
import asura.common.actor.{BaseActor, ItemActorEvent, SenderMessage}
import asura.pea.PeaConfig
import asura.pea.actor.WorkerMonitorActor.{MonitorMessage, MonitorSubscriberMessage}

/**
  * subscribe to monitor event bus
  */
class WebWorkerMonitorActor() extends BaseActor {

  PeaConfig.workerMonitorActor ! MonitorSubscriberMessage(self)
  var webActor: ActorRef = null

  override def receive: Receive = {
    case SenderMessage(sender) => webActor = sender
    case MonitorMessage(_, data) =>
      if (null != webActor) webActor ! ItemActorEvent(data)
    case _ =>
  }
}

object WebWorkerMonitorActor {

  def props() = Props(new WebWorkerMonitorActor())

  case class WebMonitorController()

}
