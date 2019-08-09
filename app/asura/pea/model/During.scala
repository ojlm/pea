package asura.pea.model

case class During(
                   value: Int,
                   unit: String,
                 )

object During {

  val TIME_UNIT_MILLI = "milli"
  val TIME_UNIT_SECOND = "second"
  val TIME_UNIT_MINUTE = "minute"
  val TIME_UNIT_HOUR = "hour"
}
