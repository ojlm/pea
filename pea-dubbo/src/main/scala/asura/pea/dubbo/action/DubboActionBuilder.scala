package asura.pea.dubbo.action

import asura.pea.dubbo.DubboCheck
import asura.pea.dubbo.protocol.{DubboComponents, DubboProtocol}
import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.session.Session
import io.gatling.core.structure.ScenarioContext

case class DubboActionBuilder[T, R](
                                     clazz: Class[T],
                                     func: (T, Session) => R,
                                     checks: List[DubboCheck[R]],
                                     protocol: Option[DubboProtocol] = None,
                                   ) extends ActionBuilder {

  private def components(protocolComponentsRegistry: ProtocolComponentsRegistry): DubboComponents =
    protocolComponentsRegistry.components(DubboProtocol.DubboProtocolKey)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val dubboComponents = components(protocolComponentsRegistry)
    new DubboAction[T, R](clazz, func, checks, dubboComponents, coreComponents, throttled, next, protocol)
  }
}
