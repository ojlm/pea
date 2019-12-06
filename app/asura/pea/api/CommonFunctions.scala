package asura.pea.api

import asura.pea.api.BaseApi.OkApiRes
import asura.pea.common.model.ApiResError
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
