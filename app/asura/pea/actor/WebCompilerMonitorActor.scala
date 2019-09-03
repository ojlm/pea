package asura.pea.actor

import akka.actor.{ActorRef, Props}
import asura.common.actor.{BaseActor, NotifyActorEvent, SenderMessage}
import asura.pea.PeaConfig
import asura.pea.actor.CompilerMonitorActor.{MonitorMessage, MonitorSubscriberMessage}

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
