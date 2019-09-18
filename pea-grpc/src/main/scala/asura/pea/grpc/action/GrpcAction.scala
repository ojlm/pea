package asura.pea.grpc.action

import io.gatling.commons.util.Clock
import io.gatling.commons.validation.Validation
import io.gatling.core.action.{Action, RequestAction}
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

case class GrpcAction[Req, Res](
                                 builder: GrpcActionBuilder[Req, Res],
                                 ctx: ScenarioContext,
                                 next: Action
                               ) extends RequestAction with NameGen {

  override def requestName: Expression[String] = builder.requestName

  override def statsEngine: StatsEngine = ctx.coreComponents.statsEngine

  override def clock: Clock = ctx.coreComponents.clock

  override def name: String = genName("grpcRequest")

  override def sendRequest(requestName: String, session: Session): Validation[Unit] = ??? // TODO
}
