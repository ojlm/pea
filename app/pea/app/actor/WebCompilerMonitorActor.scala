package pea.app.actor

import akka.actor.{ActorRef, Props}
import pea.app.PeaConfig
import pea.app.actor.CompilerMonitorActor.{MonitorMessage, MonitorSubscriberMessage}
import pea.common.actor.{BaseActor, NotifyActorEvent, SenderMessage}

class WebCompilerMonitorActor() extends BaseActor {

  PeaConfig.compilerMonitorActor ! MonitorSubscriberMessage(self)
  var webActor: ActorRef = null

  override def receive: Receive = {
    case SenderMessage(sender) => webActor = sender
    case MonitorMessage(_, data) =>
      if (null != webActor) webActor ! NotifyActorEvent(data)
    case _ =>
  }
}

object WebCompilerMonitorActor {

  def props() = Props(new WebCompilerMonitorActor())

  case class WebCompilerMonitorOptions()

}
