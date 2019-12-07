package pea.app.simulations

import io.gatling.core.Predef._
import io.grpc.netty.NettyChannelBuilder
import io.grpc.{Context, Metadata, Status}
import pea.app.gatling.PeaSimulation
import pea.grpc.Predef._
import pea.grpc.hello.{HelloRequest, HelloServiceGrpc}

class GrpcHelloSimulation extends PeaSimulation {

  override val description: String =
    """
      |Grpc simulation example
      |""".stripMargin

  val grpcProtocol = grpc(
    NettyChannelBuilder.forAddress("localhost", 50051).usePlaintext()
  )

  val TokenHeaderKey = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER)
  val TokenContextKey = Context.key[String]("token")

  val scn = scenario("grpc")
    .exec(
      grpc("Hello Pea")
        .rpc(HelloServiceGrpc.METHOD_SAY_HELLO)
        .payload(HelloRequest.defaultInstance.updateExpr(
          _.greeting :~ "pea"
        ))
        .header(TokenHeaderKey)("token")
        .check(
          statusCode is Status.Code.OK,
        )
        .extract(_.reply.some)(
          _.is("hi, pea")
        )
        .extractMultiple(_.reply.split(" ").toSeq.some)(
          _.count is 2,
          _.find(10).notExists,
          _.findAll is List("hi,", "pea")
        )
    )

  setUp(
    scn.inject(atOnceUsers(10000))
  ).protocols(grpcProtocol)
}
