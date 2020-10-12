package asura.common.util

// https://en.wikipedia.org/wiki/ANSI_escape_code
object XtermUtils {

  def redWrap(msg: String): String = {
    s"\u001b[1;31m${msg}\u001b[0m"
  }

  def greenWrap(msg: String): String = {
    s"\u001b[1;32m${msg}\u001b[0m"
  }

  def yellowWrap(msg: String): String = {
    s"\u001b[1;33m${msg}\u001b[0m"
  }

  def blueWrap(msg: String): String = {
    s"\u001b[1;34m${msg}\u001b[0m"
  }

  def magentaWrap(msg: String): String = {
    s"\u001b[1;35m${msg}\u001b[0m"
  }
}
