package pea.grpc.protocol

import io.gatling.core.protocol.ProtocolComponents
import io.gatling.core.session.Session
import io.grpc.ManagedChannel

case class GrpcComponents(
                           channel: ManagedChannel,
                         ) extends ProtocolComponents {


  override def onStart: Session => Session = ProtocolComponents.NoopOnStart

  override def onExit: Session => Unit = ProtocolComponents.NoopOnExit
}
