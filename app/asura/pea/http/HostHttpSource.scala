package asura.pea.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, Sink, Source}
import asura.pea.PeaConfig._

import scala.concurrent.Future

// connection level http cline, no pool and cache
// https://doc.akka.io/docs/akka-http/current/client-side/connection-level.html
class HostHttpSource(host: String, port: Int) {

  private val connFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnection(host = host, port = port)

  def execute(request: HttpRequest): Future[HttpResponse] = {
    val responseFuture: Future[HttpResponse] =
      Source.single(request)
        .via(connFlow)
        .runWith(Sink.head)
    responseFuture
  }
}
