package pea.dubbo.request

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Session
import pea.dubbo.DubboCheck
import pea.dubbo.action.DubboActionBuilder
import pea.dubbo.check.DubboCheckSupport
import pea.dubbo.protocol.{DubboProtocol, ProtocolModifier}

case class DubboDslBuilder[T, V](
                                  clazz: Class[T],
                                  func: (T, Session) => V,
                                  checks: List[DubboCheck[V]] = Nil,
                                  var protocol: DubboProtocol = null
                                ) extends DubboCheckSupport with ProtocolModifier {

  def check(dubboChecks: DubboCheck[V]*): DubboDslBuilder[T, V] = copy[T, V](checks = checks ::: dubboChecks.toList)

  def build(): ActionBuilder = DubboActionBuilder[T, V](clazz, func, checks, Option(protocol))
}
