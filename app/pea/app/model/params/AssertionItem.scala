package pea.app.model.params

/**
  * @param op     operation type
  * @param path   selection expression to get expect value of response, eg. jsonpath, header key
  * @param expect response expect value
  */
case class AssertionItem(
                          op: String,
                          path: String,
                          expect: Any,
                        )

object AssertionItem {
  val TYPE_EQ = "eq"
  val TYPE_JSONPATH = "jsonpath"
}
