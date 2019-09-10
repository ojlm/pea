package asura.pea.dubbo.check

import asura.pea.dubbo.{DubboCheck, DubboResponse}
import com.fasterxml.jackson.databind.ObjectMapper
import io.gatling.core.check.extractor.jsonpath.JsonPathCheckType
import io.gatling.core.check.{Check, CheckMaterializer, Preparer, Specializer}
import io.gatling.core.json.JsonParsers

object DubboJsonPathCheckMaterializer {

  private val objectMapper = new ObjectMapper()

  private def jsonPathPreparer(jsonParsers: JsonParsers): Preparer[DubboResponse[_], Any] =
    response => {
      val valueString = if (null != response.value) objectMapper.writeValueAsString(response.value) else "null"
      jsonParsers.safeParse(valueString)
    }

}

class DubboJsonPathCheckMaterializer(jsonParsers: JsonParsers) extends CheckMaterializer[JsonPathCheckType, DubboCheck[_], DubboResponse[_], Any] {

  import DubboJsonPathCheckMaterializer._

  override val specializer: Specializer[DubboCheck[_], DubboResponse[_]] = (wrapped: Check[DubboResponse[_]]) => wrapped

  override val preparer: Preparer[DubboResponse[_], Any] = jsonPathPreparer(jsonParsers)
}
