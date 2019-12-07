package pea.common.actor

import akka.actor.ActorRef

// message which wrap the sender actor
case class SenderMessage(sender: ActorRef)
