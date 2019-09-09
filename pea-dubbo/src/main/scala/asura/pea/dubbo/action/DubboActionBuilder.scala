package asura.pea.dubbo.action

import java.util.concurrent.Executors

import asura.pea.dubbo.DubboCheck
import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.structure.ScenarioContext

case class DubboActionBuilder[A](
                                  requestName: Expression[String],
                                  f: (Session) => A,
                                  checks: List[DubboCheck],
                                  threadPoolSize: Int,
                                ) extends ActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val executor = Executors.newFixedThreadPool(threadPoolSize)
    val objectMapper = new ObjectMapper()
    new DubboAction[A](requestName, f, executor, objectMapper, checks, coreComponents, throttled, next)
  }
}
