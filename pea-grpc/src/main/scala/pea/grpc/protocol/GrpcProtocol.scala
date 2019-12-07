package pea.grpc.protocol

import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{Protocol, ProtocolKey}
import io.grpc.ManagedChannelBuilder

case class GrpcProtocol(
                         channelBuilder: ManagedChannelBuilder[_],
                       ) extends Protocol

object GrpcProtocol {

  val GrpcProtocolKey = new ProtocolKey[GrpcProtocol, GrpcComponents] {
    override def protocolClass: Class[Protocol] = classOf[GrpcProtocol].asInstanceOf[Class[Protocol]]

    override def defaultProtocolValue(configuration: GatlingConfiguration) =
      throw new IllegalStateException("Can't provide a default value for GrpcProtocol")

    override def newComponents(coreComponents: CoreComponents): GrpcProtocol => GrpcComponents = { protocol =>
      val channel = protocol.channelBuilder.build()
      coreComponents.actorSystem.registerOnTermination {
        channel.shutdownNow()
      }
      GrpcComponents(channel)
    }
  }
}
