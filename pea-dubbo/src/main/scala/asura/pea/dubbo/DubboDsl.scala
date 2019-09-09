package asura.pea.dubbo

import asura.pea.dubbo.action.DubboActionBuilder
import asura.pea.dubbo.check.DubboCheckSupport
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.{Expression, Session}

trait DubboDsl extends DubboCheckSupport {

  case class DubboProcessBuilder[A](
                                     requestName: Expression[String],
                                     f: (Session) => A,
                                     checks: List[DubboCheck] = Nil,
                                     threadPoolSize: Int = 200,
                                   ) extends DubboCheckSupport {

    def check(dubboChecks: DubboCheck*): DubboProcessBuilder[A] = copy[A](checks = checks ::: dubboChecks.toList)

    def threadPoolSize(threadPoolSize: Int): DubboProcessBuilder[A] = copy[A](threadPoolSize = threadPoolSize)

    def build(): ActionBuilder = DubboActionBuilder[A](requestName, f, checks, threadPoolSize)
  }

  def dubbo[A](requestName: Expression[String], f: (Session) => A) = DubboProcessBuilder(requestName, f)

  implicit def dubboProcessBuilder2ActionBuilder[A](builder: DubboProcessBuilder[A]): ActionBuilder = builder.build()
}
