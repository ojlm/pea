package asura.pea.grpc.protocol

import io.gatling.core.protocol.ProtocolComponents
import io.gatling.core.session.Session
import io.grpc.ManagedChannelBuilder

case class GrpcComponents(
                           channelBuilder: ManagedChannelBuilder[_],
                         ) extends ProtocolComponents {

  val channel = channelBuilder.build()

  override def onStart: Session => Session = ProtocolComponents.NoopOnStart

  override def onExit: Session => Unit = { _ =>
    if (null != channel) channel.shutdownNow()
  }
}
