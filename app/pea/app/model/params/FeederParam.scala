package pea.app.model.params

case class FeederParam(
                        `type`: String,
                        path: String,
                      )

object FeederParam {

  val TYPE_CSV = "csv"
  val TYPE_JSON = "json"
}
