package pea.common

package object model {

  case class ApiReq[T](data: T)

  case class ApiRes(code: String = ApiCode.OK, msg: String = ApiMsg.SUCCESS, data: Any = null)

  object ApiResError {
    def apply(msg: String = "Error"): ApiRes = ApiRes(code = ApiCode.ERROR, msg = msg)
  }

  object ApiResInvalid {
    def apply(msg: String = "Invalid"): ApiRes = ApiRes(code = ApiCode.INVALID, msg = msg)
  }

  object ApiCode {
    val DEFAULT = "00000"
    val OK = "10000"
    val INVALID = "20000"
    val ERROR = "90000"
    val NOT_LOGIN = "90001"
    val PERMISSION_DENIED = "90002"
  }

  object ApiMsg {
    val SUCCESS = "SUCCESS"
    val FAIL = "FAIL"
    val ABORTED = "ABORTED"
    val NEED_LOGIN = "NEED LOGIN"
    val NOT_FOUND = "NOT FOUND"
    val INVALID_REQUEST_BODY = "INVALID REQUEST BODY"
    val ILLEGAL_CHARACTER = "ILLEGAL CHARACTER"
    val EMPTY_DATA = "EMPTY DATA"
  }

  object ApiType {
    val REST = "rest"
    val GRPC = "grpc"
  }

}
