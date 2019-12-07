package pea.dubbo.check

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.{CheckMaterializer, Preparer}
import io.gatling.core.json.JsonParsers
import pea.dubbo.{DubboCheck, DubboResponse}

object DubboJsonPathCheckMaterializer {

  private val objectMapper = new ObjectMapper()

  private def jsonPathPreparer(jsonParsers: JsonParsers): Preparer[DubboResponse[_], JsonNode] =
    response => {
      val valueString = if (null != response.value) objectMapper.writeValueAsString(response.value) else "null"
      jsonParsers.safeParse(valueString)
    }
}

class DubboJsonPathCheckMaterializer[V](jsonParsers: JsonParsers)
  extends CheckMaterializer[JsonPathCheckType, DubboCheck[V], DubboResponse[V], JsonNode](
    (wrapped: DubboCheck[V]) => DubboCheckModel(wrapped)
  ) {

  import DubboJsonPathCheckMaterializer._

  override val preparer: Preparer[DubboResponse[V], JsonNode] = jsonPathPreparer(jsonParsers)
}
