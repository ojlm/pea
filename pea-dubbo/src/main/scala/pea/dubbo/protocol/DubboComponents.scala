package pea.dubbo.protocol

import io.gatling.core.protocol.ProtocolComponents
import io.gatling.core.session.Session

import scala.concurrent.ExecutionContext

case class DubboComponents(
                            dubboProtocol: DubboProtocol,
                            executionContext: ExecutionContext,
                          ) extends ProtocolComponents {

  override def onStart: Session => Session = ProtocolComponents.NoopOnStart

  override def onExit: Session => Unit = ProtocolComponents.NoopOnExit
}
