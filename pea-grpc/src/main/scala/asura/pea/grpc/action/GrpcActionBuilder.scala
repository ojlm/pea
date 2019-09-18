package asura.pea.grpc.action

import asura.pea.grpc.GrpcCheck
import asura.pea.grpc.request.HeaderPair
import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.grpc.{Channel, Metadata}

import scala.concurrent.Future

case class GrpcActionBuilder[Req, Res](
                                        requestName: Expression[String],
                                        method: Channel => Req => Future[Res],
                                        payload: Expression[Req],
                                        headers: List[HeaderPair[_]] = Nil,
                                        checks: List[GrpcCheck[Res]] = Nil,
                                      ) extends ActionBuilder {

  override def build(ctx: ScenarioContext, next: Action): Action = GrpcAction(this, ctx, next)

  def header[T](key: Metadata.Key[T])(value: Expression[T]) =
    copy(headers = HeaderPair(key, value) :: headers)

  def check(checks: GrpcCheck[Res]*) =
    copy(checks = this.checks ::: checks.toList)
}
