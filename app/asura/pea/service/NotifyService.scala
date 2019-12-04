package asura.pea.service

import asura.common.util.{JsonUtils, LogUtils}
import asura.pea.PeaConfig.dispatcher
import asura.pea.http.HttpClient
import asura.pea.model.FinishedCallbackResponse
import asura.pea.model.params.FinishedCallbackRequest
import com.typesafe.scalalogging.StrictLogging

object NotifyService extends StrictLogging {

  // ignore response
  def gatlingResultCallback(request: FinishedCallbackRequest, response: FinishedCallbackResponse) = {
    val strBody = JsonUtils.stringify(response)
    logger.debug(s"Notify ${request.url} with response: ${strBody}")
    HttpClient.wsClient
      .url(request.url)
      .post(strBody)
      .recover {
        case t: Throwable =>
          logger.warn(s"Send callback(${request.url}) error: ${LogUtils.stackTraceToString(t)}")
      }
  }
}
