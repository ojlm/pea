package asura.pea.dubbo.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.core.protocol.ProtocolComponents
import io.gatling.core.session.Session

import scala.concurrent.ExecutionContext

case class DubboComponents(
                            dubboProtocol: DubboProtocol,
                            executionContext: ExecutionContext,
                            objectMapper: ObjectMapper = new ObjectMapper(),
                          ) extends ProtocolComponents {

  override def onStart: Session => Session = ProtocolComponents.NoopOnStart

  override def onExit: Session => Unit = ProtocolComponents.NoopOnExit
}
