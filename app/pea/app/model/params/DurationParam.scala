package pea.app.model.params

case class DurationParam(
                          value: Int,
                          unit: String,
                        )

object DurationParam {

  val TIME_UNIT_MILLI = "milli"
  val TIME_UNIT_SECOND = "second"
  val TIME_UNIT_MINUTE = "minute"
  val TIME_UNIT_HOUR = "hour"
}
