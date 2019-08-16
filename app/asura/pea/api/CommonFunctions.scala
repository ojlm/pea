package asura.pea.api

import asura.common.model.ApiResError
import asura.play.api.BaseApi.OkApiRes
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
