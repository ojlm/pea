package pea.common.exceptions

import pea.common.exceptions.ErrorMessages.ErrorMessage

import scala.concurrent.Future

trait ErrorMessages {

  val error_ServerError = ErrorMessage("Server Error")("error_ServerError")
  val error_InvalidRequestParameters = ErrorMessage("Invalid request parameters")("error_InvalidRequestParameters")

  def error_Throwable(t: Throwable) = ErrorMessage(t.getMessage, t)("error_Throwable")

  def error_Msgs(msgs: Seq[String]) = ErrorMessage(msgs.mkString(","))("error_Msgs")

  def error_IllegalCharacter(msg: String) = ErrorMessage(msg)("error_IllegalCharacter")
}

object ErrorMessages {

  case class ErrorMessage(val errMsg: String, val t: Throwable = null)(_name: String) {

    def toException: ErrorMessageException = {
      ErrorMessageException(this)
    }

    def toFutureFail: Future[Nothing] = {
      Future.failed(ErrorMessageException(this))
    }

    val name = _name
  }

  case class ErrorMessageException(error: ErrorMessages.ErrorMessage) extends RuntimeException(error.errMsg)

}
