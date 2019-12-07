package pea.dubbo.action

import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.gatling.core.session.Session
import io.gatling.core.structure.ScenarioContext
import pea.dubbo.DubboCheck
import pea.dubbo.protocol.{DubboComponents, DubboProtocol}

case class DubboActionBuilder[T, V](
                                     clazz: Class[T],
                                     func: (T, Session) => V,
                                     checks: List[DubboCheck[V]],
                                     protocol: Option[DubboProtocol] = None,
                                   ) extends ActionBuilder {

  private def components(protocolComponentsRegistry: ProtocolComponentsRegistry): DubboComponents =
    protocolComponentsRegistry.components(DubboProtocol.DubboProtocolKey)

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val dubboComponents = components(protocolComponentsRegistry)
    new DubboAction[T, V](clazz, func, checks, dubboComponents, coreComponents, throttled, next, protocol)
  }
}
