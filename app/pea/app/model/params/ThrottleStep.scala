package pea.app.model.params

case class ThrottleStep(
                         `type`: String,
                         rps: Int,
                         duration: DurationParam = null,
                       )

object ThrottleStep {

  val TYPE_REACH = "reach"
  val TYPE_HOLD = "hold"
  val TYPE_JUMP = "jump"
}
