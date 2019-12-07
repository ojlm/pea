package pea.app.http

import pea.app.PeaConfig._
import play.api.libs.ws.ahc.StandaloneAhcWSClient

object HttpClient {

  lazy val wsClient = StandaloneAhcWSClient()

  def close(): Unit = {
    if (null != wsClient) {
      wsClient.close()
    }
  }
}
