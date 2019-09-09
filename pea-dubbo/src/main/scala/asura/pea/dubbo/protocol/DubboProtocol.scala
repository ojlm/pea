package asura.pea.dubbo.protocol

import java.util.concurrent.Executors

import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{Protocol, ProtocolKey}

import scala.concurrent.ExecutionContext

case class DubboProtocol(
                          application: Option[String] = Some("pea-dubbo-consumer"),
                          group: Option[String] = None,
                          version: Option[String] = None,
                          endpointUrl: Option[String] = None,
                          registryUrl: Option[String] = None,
                          threads: Int = 200,
                          timeout: Option[Int] = None,
                        ) extends Protocol {

  type Components = DubboComponents
}

object DubboProtocol {

  val DubboProtocolKey: ProtocolKey[DubboProtocol, DubboComponents] = new ProtocolKey[DubboProtocol, DubboComponents] {

    def protocolClass: Class[io.gatling.core.protocol.Protocol] = classOf[DubboProtocol].asInstanceOf[Class[io.gatling.core.protocol.Protocol]]

    def defaultProtocolValue(configuration: GatlingConfiguration): DubboProtocol = throw new IllegalStateException("Can't provide a default value for DubboProtocol")

    def newComponents(coreComponents: CoreComponents): DubboProtocol => DubboComponents = {
      dubboProtocol => {
        val executor = Executors.newFixedThreadPool(dubboProtocol.threads)
        coreComponents.actorSystem.registerOnTermination {
          executor.shutdown()
        }
        val executionContext = ExecutionContext.fromExecutor(executor)
        DubboComponents(dubboProtocol, executionContext)
      }
    }
  }
}
