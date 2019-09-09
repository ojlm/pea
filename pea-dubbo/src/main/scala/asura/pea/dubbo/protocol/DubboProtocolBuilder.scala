package asura.pea.dubbo.protocol

import com.softwaremill.quicklens._
import io.gatling.core.config.GatlingConfiguration

case class DubboProtocolBuilder(protocol: DubboProtocol) {

  def application(name: String): DubboProtocolBuilder = this.modify(_.protocol.application).setTo(Option(name))

  def group(name: String): DubboProtocolBuilder = this.modify(_.protocol.group).setTo(Option(name))

  def version(name: String): DubboProtocolBuilder = this.modify(_.protocol.version).setTo(Option(name))

  def endpoint(url: String): DubboProtocolBuilder = this.modify(_.protocol.endpointUrl).setTo(Option(url))

  def endpoint(address: String, port: Int): DubboProtocolBuilder =
    this.modify(_.protocol.endpointUrl).setTo(Some(s"dubbo://${address}:${port}/"))

  def zookeeper(url: String): DubboProtocolBuilder = this.modify(_.protocol.registryUrl).setTo(Option(url))

  def zookeeper(address: String, port: Int = 2181): DubboProtocolBuilder =
    this.modify(_.protocol.registryUrl).setTo(Some(s"zookeeper://${address}:${port}"))

  def threads(count: Int): DubboProtocolBuilder = this.modify(_.protocol.threads).setTo(count)

  def timeout(count: Int): DubboProtocolBuilder = this.modify(_.protocol.timeout).setTo(Some(count))

  def build = protocol
}

object DubboProtocolBuilder {

  implicit def toDubboProtocol(builder: DubboProtocolBuilder): DubboProtocol = builder.build

  def apply(configuration: GatlingConfiguration): DubboProtocol = {
    DubboProtocol()
  }
}
