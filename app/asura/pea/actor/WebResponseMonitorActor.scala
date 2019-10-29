package asura.pea.actor

import akka.actor.{ActorRef, Props}
import asura.common.actor.{BaseActor, NotifyActorEvent, SenderMessage}
import asura.pea.PeaConfig
import asura.pea.actor.ResponseMonitorActor.{ResponseMessage, ResponseSubscriberMessage}

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
