package pea.app.hook

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import pea.app.api.BaseApi.OkApiRes
import pea.common.exceptions.ErrorMessages
import pea.common.exceptions.ErrorMessages.ErrorMessageException
import pea.common.model.{ApiCode, ApiRes, ApiResError}
import pea.common.util.{LogUtils, StringUtils}
import play.api.http.HttpErrorHandler
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject()(messagesApi: MessagesApi, langs: Langs) extends HttpErrorHandler with ErrorMessages {

  lazy val logger = LoggerFactory.getLogger(classOf[ErrorHandler])

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val msg = s""""${request.method} ${request.uri}" ${statusCode} ${if (StringUtils.isNotEmpty(message)) message else ""}"""
    Future.successful(OkApiRes(ApiResError(msg)))
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    val logStack = LogUtils.stackTraceToString(exception)
    logger.warn(logStack)
    val requestLocal = request.headers.get("Local")
    implicit val lang = if (requestLocal.nonEmpty) {
      langs.availables.find(_.code == requestLocal.get).getOrElse(langs.availables.head)
    } else {
      langs.availables.head
    }
    exception match {
      case errMsgException: ErrorMessageException =>
        val errMsg = messagesApi(errMsgException.error.name, errMsgException.error.errMsg)
        Future.successful(OkApiRes(ApiRes(code = ApiCode.ERROR, msg = errMsg, data = logStack)))
      case _ =>
        val message = if (StringUtils.isNotEmpty(exception.getMessage)) {
          exception.getMessage
        } else {
          messagesApi(error_ServerError.name)
        }
        Future.successful(OkApiRes(ApiRes(code = ApiCode.ERROR, msg = message, data = logStack)))
    }
  }
}
