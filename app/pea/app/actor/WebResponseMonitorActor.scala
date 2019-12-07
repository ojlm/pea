package pea.app.actor

import akka.actor.{ActorRef, Props}
import pea.app.PeaConfig
import pea.app.actor.ResponseMonitorActor.{ResponseMessage, ResponseSubscriberMessage}
import pea.common.actor.{BaseActor, NotifyActorEvent, SenderMessage}

class WebResponseMonitorActor() extends BaseActor {

  PeaConfig.responseMonitorActor ! ResponseSubscriberMessage(self)
  var webActor: ActorRef = null

  override def receive: Receive = {
    case SenderMessage(sender) => webActor = sender
    case ResponseMessage(_, data) =>
      if (null != webActor) webActor ! NotifyActorEvent(data)
    case _ =>
  }
}

object WebResponseMonitorActor {

  def props() = Props(new WebResponseMonitorActor())

  case class WebResponseMonitorOptions()

}
