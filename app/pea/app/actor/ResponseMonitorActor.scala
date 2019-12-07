package pea.app.actor

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{ActorClassifier, ActorEventBus, ManagedActorClassification}
import pea.app.actor.ResponseMonitorActor.{ResponseMessage, ResponseMonitorBus, ResponseSubscriberMessage}
import pea.common.actor.BaseActor
import pea.common.util.XtermUtils

class ResponseMonitorActor extends BaseActor {

  val monitorBus = new ResponseMonitorBus(context.system)

  override def receive: Receive = {
    case ResponseSubscriberMessage(ref) =>
      monitorBus.subscribe(ref, self)
    case data: String =>
      monitorBus.publish(ResponseMessage(self, data))
    case message: Any =>
      log.warning(s"Unknown message type ${message}")
  }
}

object ResponseMonitorActor {

  def props() = Props(new ResponseMonitorActor())

  case class ResponseSubscriberMessage(ref: ActorRef)

  case class ResponseMessage(ref: ActorRef, data: String)

  class ResponseMonitorBus(val system: ActorSystem) extends ActorEventBus with ActorClassifier with ManagedActorClassification {

    override type Event = ResponseMessage

    override protected def classify(event: ResponseMessage): ActorRef = event.ref

    override protected def mapSize: Int = 1
  }

  def formatResponse(status: Int, response: String): String = {
    s"""
       |${if (status != 200) XtermUtils.redWrap(status.toString) else XtermUtils.greenWrap(status.toString)}
       |${XtermUtils.blueWrap(response)}
       |""".stripMargin
  }
}
