package pea.grpc

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture, MoreExecutors}
import io.gatling.commons.NotNothing
import io.gatling.commons.validation.{Failure, Success}
import io.gatling.core.session.Expression
import io.gatling.core.session.el.ElMessages
import io.grpc.stub.{AbstractStub, ClientCalls}
import io.grpc.{CallOptions, Channel, ManagedChannelBuilder, MethodDescriptor}
import pea.grpc.action.GrpcActionBuilder
import pea.grpc.protocol.GrpcProtocol

import scala.concurrent.{Future, Promise}
import scala.reflect.ClassTag

trait GrpcDsl {

  def grpc(channelBuilder: ManagedChannelBuilder[_]) = GrpcProtocol(channelBuilder)

  def grpc(requestName: Expression[String]) = new Call(requestName)

  class Call(requestName: Expression[String]) {
    def service[Service <: AbstractStub[Service]](stub: Channel => Service) = new CallWithService[Service](requestName, stub)

    def rpc[Req, Res](method: MethodDescriptor[Req, Res]) = {
      assert(method.getType == MethodDescriptor.MethodType.UNARY)
      new CallWithMethod[Req, Res](requestName, method)
    }
  }

  class CallWithMethod[Req, Res](requestName: Expression[String], method: MethodDescriptor[Req, Res]) {
    val f = (channel: Channel) => {
      request: Req => guavaFuture2ScalaFuture(ClientCalls.futureUnaryCall(channel.newCall(method, CallOptions.DEFAULT), request))
    }

    def payload(req: Expression[Req]) = GrpcActionBuilder(requestName, f, req)
  }

  class CallWithService[Service <: AbstractStub[Service]](requestName: Expression[String], stub: Channel => Service) {
    def rpc[Req, Res](func: Service => Req => Future[Res])(request: Expression[Req]) =
      GrpcActionBuilder(requestName, stub andThen (func), request)
  }

  def $[T: ClassTag : NotNothing](name: String): Expression[T] = s => s.attributes.get(name) match {
    case Some(t: T) => Success(t)
    case None => ElMessages.undefinedSessionAttribute(name)
    case Some(t) => Failure(s"Value $t is of type ${t.getClass.getName}, expected ${implicitly[ClassTag[T]].runtimeClass.getName}")
  }

  def guavaFuture2ScalaFuture[Res](guavaFuture: ListenableFuture[Res]): Future[Res] = {
    val p = Promise[Res]()
    Futures.addCallback(
      guavaFuture,
      new FutureCallback[Res] {
        override def onFailure(t: Throwable): Unit = p.failure(t)

        override def onSuccess(a: Res): Unit = p.success(a)
      },
      MoreExecutors.directExecutor()
    )
    p.future
  }
}
