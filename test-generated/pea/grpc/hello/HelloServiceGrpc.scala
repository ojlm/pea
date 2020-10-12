package pea.grpc.hello

object HelloServiceGrpc {
  val METHOD_SAY_HELLO: _root_.io.grpc.MethodDescriptor[pea.grpc.hello.HelloRequest, pea.grpc.hello.HelloResponse] =
    _root_.io.grpc.MethodDescriptor.newBuilder()
      .setType(_root_.io.grpc.MethodDescriptor.MethodType.UNARY)
      .setFullMethodName(_root_.io.grpc.MethodDescriptor.generateFullMethodName("pea.grpc.HelloService", "SayHello"))
      .setSampledToLocalTracing(true)
      .setRequestMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pea.grpc.hello.HelloRequest])
      .setResponseMarshaller(_root_.scalapb.grpc.Marshaller.forMessage[pea.grpc.hello.HelloResponse])
      .setSchemaDescriptor(_root_.scalapb.grpc.ConcreteProtoMethodDescriptorSupplier.fromMethodDescriptor(pea.grpc.hello.HelloProto.javaDescriptor.getServices.get(0).getMethods.get(0)))
      .build()
  
  val SERVICE: _root_.io.grpc.ServiceDescriptor =
    _root_.io.grpc.ServiceDescriptor.newBuilder("pea.grpc.HelloService")
      .setSchemaDescriptor(new _root_.scalapb.grpc.ConcreteProtoFileDescriptorSupplier(pea.grpc.hello.HelloProto.javaDescriptor))
      .addMethod(METHOD_SAY_HELLO)
      .build()
  
  trait HelloService extends _root_.scalapb.grpc.AbstractService {
    override def serviceCompanion = HelloService
    def sayHello(request: pea.grpc.hello.HelloRequest): scala.concurrent.Future[pea.grpc.hello.HelloResponse]
  }
  
  object HelloService extends _root_.scalapb.grpc.ServiceCompanion[HelloService] {
    implicit def serviceCompanion: _root_.scalapb.grpc.ServiceCompanion[HelloService] = this
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = pea.grpc.hello.HelloProto.javaDescriptor.getServices.get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.ServiceDescriptor = pea.grpc.hello.HelloProto.scalaDescriptor.services(0)
    def bindService(serviceImpl: HelloService, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition =
      _root_.io.grpc.ServerServiceDefinition.builder(SERVICE)
      .addMethod(
        METHOD_SAY_HELLO,
        _root_.io.grpc.stub.ServerCalls.asyncUnaryCall(new _root_.io.grpc.stub.ServerCalls.UnaryMethod[pea.grpc.hello.HelloRequest, pea.grpc.hello.HelloResponse] {
          override def invoke(request: pea.grpc.hello.HelloRequest, observer: _root_.io.grpc.stub.StreamObserver[pea.grpc.hello.HelloResponse]): Unit =
            serviceImpl.sayHello(request).onComplete(scalapb.grpc.Grpc.completeObserver(observer))(
              executionContext)
        }))
      .build()
  }
  
  trait HelloServiceBlockingClient {
    def serviceCompanion = HelloService
    def sayHello(request: pea.grpc.hello.HelloRequest): pea.grpc.hello.HelloResponse
  }
  
  class HelloServiceBlockingStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[HelloServiceBlockingStub](channel, options) with HelloServiceBlockingClient {
    override def sayHello(request: pea.grpc.hello.HelloRequest): pea.grpc.hello.HelloResponse = {
      _root_.scalapb.grpc.ClientCalls.blockingUnaryCall(channel, METHOD_SAY_HELLO, options, request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): HelloServiceBlockingStub = new HelloServiceBlockingStub(channel, options)
  }
  
  class HelloServiceStub(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions = _root_.io.grpc.CallOptions.DEFAULT) extends _root_.io.grpc.stub.AbstractStub[HelloServiceStub](channel, options) with HelloService {
    override def sayHello(request: pea.grpc.hello.HelloRequest): scala.concurrent.Future[pea.grpc.hello.HelloResponse] = {
      _root_.scalapb.grpc.ClientCalls.asyncUnaryCall(channel, METHOD_SAY_HELLO, options, request)
    }
    
    override def build(channel: _root_.io.grpc.Channel, options: _root_.io.grpc.CallOptions): HelloServiceStub = new HelloServiceStub(channel, options)
  }
  
  def bindService(serviceImpl: HelloService, executionContext: scala.concurrent.ExecutionContext): _root_.io.grpc.ServerServiceDefinition = HelloService.bindService(serviceImpl, executionContext)
  
  def blockingStub(channel: _root_.io.grpc.Channel): HelloServiceBlockingStub = new HelloServiceBlockingStub(channel)
  
  def stub(channel: _root_.io.grpc.Channel): HelloServiceStub = new HelloServiceStub(channel)
  
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.ServiceDescriptor = pea.grpc.hello.HelloProto.javaDescriptor.getServices.get(0)
  
}