package asura.pea

import asura.pea.common.exceptions.ErrorMessages.ErrorMessage
import asura.pea.common.exceptions.{ErrorMessages => CommonErrorMessages}

object ErrorMessages extends CommonErrorMessages {

  val error_BusyStatus = ErrorMessage("Node is busy")("error_BusyStatus")
}
