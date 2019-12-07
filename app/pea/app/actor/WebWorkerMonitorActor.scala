package pea.app.actor

import akka.actor.{ActorRef, Props}
import pea.app.PeaConfig
import pea.app.actor.WorkerMonitorActor.{MonitorMessage, MonitorSubscriberMessage}
import pea.common.actor.{BaseActor, ItemActorEvent, SenderMessage}

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

  case class WebWorkerMonitorOptions()

}
