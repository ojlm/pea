package asura.pea.grpc.hello

object HelloServiceGrpc {
  val METHOD_SAY_HELLO: _root_.io.grpc.MethodDescriptor[asura.pea.grpc.hello.HelloRequest, asura.pea.grpc.hello.HelloResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("asura.pea.grpc.HelloService", "SayHello"))
      .setRequestMarshaller(new com.trueaccord.scalapb.grpc.Marshaller(asura.pea.grpc.hello.HelloRequest))
      .setResponseMarshaller(new com.trueaccord.scalapb.grpc.Marshaller(asura.pea.grpc.hello.HelloResponse))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("asura.pea.grpc.HelloService")
      .setSchemaDescriptor(new _root_.com.trueaccord.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(asura.pea.grpc.hello.HelloProto.javaDescriptor))
      .addMethod(METHOD_SAY_HELLO)
      .build()
  
  trait HelloService extends _root_.com.trueaccord.scalapb.grpc.AbstractService {
    override def serviceCompanion = HelloService
    def sayHello(request: asura.pea.grpc.hello.HelloRequest): scala.concurrent.Future[asura.pea.grpc.hello.HelloResponse]
  }
  
  object HelloService extends _root_.com.trueaccord.scalapb.grpc.ServiceCompanion[HelloService] {
    implicit def serviceCompanion: _root_.com.trueaccord.scalapb.grpc.ServiceCompanion[HelloService] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = asura.pea.grpc.hello.HelloProto.javaDescriptor.getServices().get(0)
  }
  
  trait HelloServiceBlockingClient {
    def serviceCompanion = HelloService
    def sayHello(request: asura.pea.grpc.hello.HelloRequest): asura.pea.grpc.hello.HelloResponse
  }
  
  class HelloServiceBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[HelloServiceBlockingStub](channel, options) with HelloServiceBlockingClient {
    override def sayHello(request: asura.pea.grpc.hello.HelloRequest): asura.pea.grpc.hello.HelloResponse = {
      _root_.io.grpc.stub.ClientCalls.blockingUnaryCall(channel.newCall(METHOD_SAY_HELLO, options), request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): HelloServiceBlockingStub = new HelloServiceBlockingStub(channel, options)
  }
  
  class HelloServiceStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[HelloServiceStub](channel, options) with HelloService {
    override def sayHello(request: asura.pea.grpc.hello.HelloRequest): scala.concurrent.Future[asura.pea.grpc.hello.HelloResponse] = {
      com.trueaccord.scalapb.grpc.Grpc.guavaFuture2ScalaFuture(_root_.io.grpc.stub.ClientCalls.futureUnaryCall(channel.newCall(METHOD_SAY_HELLO, options), request))
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): HelloServiceStub = new HelloServiceStub(channel, options)
  }
  
  def bindService(serviceImpl: HelloService, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
    _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
    .addMethod(
      METHOD_SAY_HELLO,
      _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[asura.pea.grpc.hello.HelloRequest, asura.pea.grpc.hello.HelloResponse] {
        override def invoke(request: asura.pea.grpc.hello.HelloRequest, observer: _root_.io.grpc.stub.StreamObserver[asura.pea.grpc.hello.HelloResponse]): Unit =
          serviceImpl.sayHello(request).onComplete(com.trueaccord.scalapb.grpc.Grpc.completeObserver(observer))(
            executionContext)
      }))
    .build()
  
  def blockingStub(channel: _root_.io.grpc.Channel): HelloServiceBlockingStub = new HelloServiceBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): HelloServiceStub = new HelloServiceStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = asura.pea.grpc.hello.HelloProto.javaDescriptor.getServices().get(0)
  
}