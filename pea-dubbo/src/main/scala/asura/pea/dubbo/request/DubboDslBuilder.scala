package asura.pea.dubbo.request

import asura.pea.dubbo.DubboCheck
import asura.pea.dubbo.action.DubboActionBuilder
import asura.pea.dubbo.check.DubboCheckSupport
import asura.pea.dubbo.protocol.{DubboProtocol, ProtocolModifier}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Session

case class DubboDslBuilder[T, V](
                                  clazz: Class[T],
                                  func: (T, Session) => V,
                                  checks: List[DubboCheck[V]] = Nil,
                                  var protocol: DubboProtocol = null
                                ) extends DubboCheckSupport with ProtocolModifier {

  def check(dubboChecks: DubboCheck[V]*): DubboDslBuilder[T, V] = copy[T, V](checks = checks ::: dubboChecks.toList)

  def build(): ActionBuilder = DubboActionBuilder[T, V](clazz, func, checks, Option(protocol))
}
