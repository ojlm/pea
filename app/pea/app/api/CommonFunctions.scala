package pea.app.api

import pea.app.api.BaseApi.OkApiRes
import pea.common.model.ApiResError
import play.api.mvc.Result

import scala.concurrent.Future

trait CommonFunctions {

  object ErrorResult {
    def apply(msg: String): Result = {
      OkApiRes(ApiResError(msg))
    }
  }

  object FutureErrorResult {
    def apply(msg: String): Future[Result] = {
      Future.successful(ErrorResult(msg))
    }
  }

}
