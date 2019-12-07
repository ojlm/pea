package pea.common.util

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

object DateUtils {

  val DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

  def nowTimestamp(): Timestamp = Timestamp.valueOf(LocalDateTime.now())

  def nowDateTime: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_PATTERN))

  def parse(time: Long, pattern: String = DEFAULT_DATE_TIME_PATTERN): String = {
    val sdf = new SimpleDateFormat(pattern)
    sdf.format(new Date(time))
  }
}
