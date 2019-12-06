package asura.pea.grpc

import java.util.concurrent.TimeUnit

import asura.pea.common.util.LogUtils
import asura.pea.grpc.hello.HelloServiceGrpc.HelloServiceBlockingStub
import asura.pea.grpc.hello.{HelloRequest, HelloServiceGrpc}
import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder

object HelloServiceClient {

  def main(args: Array[String]): Unit = {
    val channel = NettyChannelBuilder
      .forAddress("localhost", 50051)
      .usePlaintext()
      .build()
    val blockingStub = HelloServiceGrpc.blockingStub(channel)
    val client = new HelloServiceClient(channel, blockingStub)
    client.greet("pea")
    client.shutdown()
  }
}

class HelloServiceClient(
                          channel: ManagedChannel,
                          blockingStub: HelloServiceBlockingStub,
                        ) {

  def shutdown(): Unit = {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }

  def greet(name: String): Unit = {
    val request = HelloRequest("pea")
    try {
      val response = blockingStub.sayHello(request)
      println(s"GOT: ${response.reply}")
    } catch {
      case t: Throwable => println(LogUtils.stackTraceToString(t))
    }
  }
}
