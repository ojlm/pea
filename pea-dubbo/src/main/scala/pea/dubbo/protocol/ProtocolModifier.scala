package pea.dubbo.protocol

import com.softwaremill.quicklens._

trait ProtocolModifier {

  var protocol: DubboProtocol

  def application(name: String): this.type = {
    initProtocol()
    protocol = protocol.modify(_.application).setTo(Option(name))
    this
  }

  def group(name: String): this.type = {
    initProtocol()
    protocol = protocol.modify(_.group).setTo(Option(name))
    this
  }

  def version(name: String): this.type = {
    initProtocol()
    protocol = protocol.modify(_.version).setTo(Option(name))
    this
  }

  def endpoint(url: String): this.type = {
    initProtocol()
    protocol = protocol.modify(_.endpointUrl).setTo(Option(url))
    this
  }

  def endpoint(address: String, port: Int): this.type = {
    initProtocol()
    protocol = protocol.modify(_.endpointUrl).setTo(Some(s"dubbo://${address}:${port}/"))
    this
  }

  def zookeeper(url: String): this.type = {
    initProtocol()
    protocol = protocol.modify(_.registryUrl).setTo(Option(url))
    this
  }

  def zookeeper(address: String, port: Int): this.type = {
    initProtocol()
    protocol = protocol.modify(_.registryUrl).setTo(Some(s"zookeeper://${address}:${port}"))
    this
  }

  def threads(count: Int): this.type = {
    initProtocol()
    protocol = protocol.modify(_.threads).setTo(count)
    this
  }

  def timeout(count: Int): this.type = {
    initProtocol()
    protocol = protocol.modify(_.timeout).setTo(Some(count))
    this
  }

  @inline def initProtocol(): Unit = {
    if (null == protocol) {
      protocol = DubboProtocol()
    }
  }
}
