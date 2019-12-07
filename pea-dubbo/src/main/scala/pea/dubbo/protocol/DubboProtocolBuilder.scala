package pea.dubbo.protocol

import io.gatling.core.config.GatlingConfiguration

case class DubboProtocolBuilder(var protocol: DubboProtocol) extends ProtocolModifier {

  def build = protocol
}

object DubboProtocolBuilder {

  def apply(implicit configuration: GatlingConfiguration): DubboProtocolBuilder = {
    DubboProtocolBuilder(DubboProtocol(configuration))
  }
}
