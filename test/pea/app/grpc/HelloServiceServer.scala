package pea.app.grpc

import io.grpc.Server
import io.grpc.netty.NettyServerBuilder
import pea.grpc.hello.{HelloRequest, HelloResponse, HelloServiceGrpc}

import scala.concurrent.{ExecutionContext, Future}

object HelloServiceServer {

  def main(args: Array[String]): Unit = {
    val server = new HelloServiceServer(ExecutionContext.global)
    server.start(50051)
    server.blockUntilShutdown()
  }
}

class HelloServiceServer(executionContext: ExecutionContext) {

  private var server: Server = null

  private def start(port: Int): Unit = {
    server = NettyServerBuilder.forPort(port).addService(HelloServiceGrpc.bindService(new HelloServiceImpl, executionContext)).build().start()
    println(s"Server start at: ${server.getPort}")
    sys.addShutdownHook {
      stop()
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class HelloServiceImpl extends HelloServiceGrpc.HelloService {
    override def sayHello(request: HelloRequest): Future[HelloResponse] = {
      val response = HelloResponse(s"hi, ${request.greeting}")
      Future.successful(response)
    }
  }

}
