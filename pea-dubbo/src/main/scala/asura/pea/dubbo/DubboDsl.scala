package asura.pea.dubbo

import asura.pea.dubbo.protocol.{DubboProtocol, DubboProtocolBuilder}
import asura.pea.dubbo.request.DubboDslBuilder
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session

trait DubboDsl {

  def dubbo(implicit configuration: GatlingConfiguration) = DubboProtocolBuilder(configuration)

  implicit def protocolBuilder2Protocol(builder: DubboProtocolBuilder): DubboProtocol = builder.build

  def invoke[T, R](clazz: Class[T])(func: (T, Session) => R): DubboDslBuilder[T, R] = {
    DubboDslBuilder(clazz, func)
  }

  implicit def dubboDslBuilder2ActionBuilder[T, R](builder: DubboDslBuilder[T, R]): ActionBuilder = builder.build()
}
