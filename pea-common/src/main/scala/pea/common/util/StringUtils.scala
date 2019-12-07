package pea.common.util

object StringUtils {

  val EMPTY = ""

  def isEmpty(value: String): Boolean = null == value || value.length == 0

  def hasEmpty(fist: String, rest: String*): Boolean = {
    var hasEmpty = false
    if (isEmpty(fist)) hasEmpty = true
    for (v <- rest if !hasEmpty) {
      if (isEmpty(v)) hasEmpty = true
    }
    hasEmpty
  }

  def isEmpty(value: Option[String]): Boolean = {
    if (value.nonEmpty) {
      isEmpty(value.get)
    } else {
      false
    }
  }

  def isNotEmpty(value: String): Boolean = !isEmpty(value)

  def isNotEmpty(value: Option[String]): Boolean = !isEmpty(value)

  def notEmptyElse(value: String, default: String): String = if (isNotEmpty(value)) value else default

  def toOption(value: String): Option[String] = if (isEmpty(value)) None else Some(value)
}
