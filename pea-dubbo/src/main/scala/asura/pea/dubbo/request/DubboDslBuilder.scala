package asura.pea.dubbo.request

import asura.pea.dubbo.DubboCheck
import asura.pea.dubbo.action.DubboActionBuilder
import asura.pea.dubbo.check.DubboCheckSupport
import asura.pea.dubbo.protocol.{DubboProtocol, ProtocolModifier}
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Session

case class DubboDslBuilder[T, R](
                                  clazz: Class[T],
                                  func: (T, Session) => R,
                                  checks: List[DubboCheck[R]] = Nil,
                                  var protocol: DubboProtocol = null
                                ) extends DubboCheckSupport with ProtocolModifier {

  def check(dubboChecks: DubboCheck[R]*): DubboDslBuilder[T, R] = copy[T, R](checks = checks ::: dubboChecks.toList)

  def build(): ActionBuilder = DubboActionBuilder[T, R](clazz, func, checks, Option(protocol))
}
