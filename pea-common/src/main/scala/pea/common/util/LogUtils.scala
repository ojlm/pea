package pea.common.util

import java.io.{PrintWriter, StringWriter}

object LogUtils {

  def stackTraceToString(t: Throwable): String = {
    val sw = new StringWriter()
    t.printStackTrace(new PrintWriter(sw))
    sw.toString
  }
}
