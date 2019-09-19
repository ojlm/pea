package asura.pea.grpc.action

import asura.pea.grpc.check.StatusExtract
import asura.pea.grpc.protocol.{GrpcComponents, GrpcProtocol}
import asura.pea.grpc.request.HeaderPair
import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.Clock
import io.gatling.commons.validation.{Failure, Success, SuccessWrapper, Validation}
import io.gatling.core.action.{Action, RequestAction}
import io.gatling.core.check.Check
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.grpc.stub.MetadataUtils
import io.grpc.{Channel, ClientInterceptors, Metadata}

case class GrpcAction[Req, Res](
                                 builder: GrpcActionBuilder[Req, Res],
                                 ctx: ScenarioContext,
                                 next: Action
                               ) extends RequestAction with NameGen {

  override def requestName: Expression[String] = builder.requestName

  override def statsEngine: StatsEngine = ctx.coreComponents.statsEngine

  override def clock: Clock = ctx.coreComponents.clock

  override def name: String = genName("grpcRequest")

  private val components: GrpcComponents = ctx.protocolComponentsRegistry.components(GrpcProtocol.GrpcProtocolKey)

  override def sendRequest(requestName: String, session: Session): Validation[Unit] = {
    type ResolvedHeader = (Metadata.Key[T], T) forSome {type T}
    for {
      resolvedHeaders <- builder.headers.foldLeft[Validation[List[ResolvedHeader]]](Nil.success) { case (lv, HeaderPair(key, value)) =>
        for {
          l <- lv
          resolvedValue <- value(session)
        } yield (key, resolvedValue) :: l
      }
      resolvedPayload <- builder.payload(session)
    } yield {
      val rawChannel = components.channel
      val channel = if (resolvedHeaders.isEmpty) rawChannel else {
        val headers = new Metadata()
        resolvedHeaders.foreach { case (key, value) => headers.put(key, value) }
        ClientInterceptors.intercept(rawChannel, MetadataUtils.newAttachHeadersInterceptor(headers))
      }
      if (ctx.throttled) {
        ctx.coreComponents.throttler.throttle(session.scenario, () => {
          run(channel, resolvedPayload, session, requestName)
        })
      } else {
        run(channel, resolvedPayload, session, requestName)
      }
    }
  }

  private def run(channel: Channel, payload: Req, session: Session, resolvedRequestName: String): Unit = {
    implicit val ec = ctx.coreComponents.actorSystem.dispatcher
    val start = clock.nowMillis
    builder.method(channel)(payload).onComplete(response => {
      val end = clock.nowMillis
      val resolvedChecks = if (builder.checks.exists(_.checksStatus)) builder.checks else {
        StatusExtract.DefaultCheck :: builder.checks
      }
      val (checkSaveUpdated, checkError) = Check.check(response, session, resolvedChecks, null)
      val (status, newSession) = if (checkError.isEmpty) {
        (OK, checkSaveUpdated)
      } else {
        (KO, checkSaveUpdated.markAsFailed)
      }
      statsEngine.logResponse(
        newSession,
        resolvedRequestName,
        start,
        end,
        status,
        StatusExtract.extractStatus(response) match {
          case Success(value) => Some(value.getCode.toString)
          case Failure(_) => None
        },
        checkError.map(_.message)
      )
      next ! newSession
    })
  }
}
