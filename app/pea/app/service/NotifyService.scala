package pea.app.service

import com.typesafe.scalalogging.StrictLogging
import pea.app.PeaConfig.dispatcher
import pea.app.http.HttpClient
import pea.app.model.FinishedCallbackResponse
import pea.app.model.params.FinishedCallbackRequest
import pea.common.util.{JsonUtils, LogUtils}

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
