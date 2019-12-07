package pea.app

import pea.common.exceptions.ErrorMessages.ErrorMessage
import pea.common.exceptions.{ErrorMessages => CommonErrorMessages}

object ErrorMessages extends CommonErrorMessages {

  val error_BusyStatus = ErrorMessage("Node is busy")("error_BusyStatus")
}
