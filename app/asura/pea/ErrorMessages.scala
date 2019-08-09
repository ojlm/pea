package asura.pea

import asura.common.exceptions.ErrorMessages.ErrorMessage
import asura.common.exceptions.{ErrorMessages => CommonErrorMessages}

object ErrorMessages extends CommonErrorMessages {

  val error_BusyStatus = ErrorMessage("Node is busy")("error_BusyStatus")
}
