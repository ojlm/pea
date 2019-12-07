package pea.dubbo.check

import com.fasterxml.jackson.databind.JsonNode
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.{CheckBuilder, CheckMaterializer, FindCheckBuilder, ValidatorCheckBuilder}
import io.gatling.core.json.JsonParsers
import pea.dubbo.{DubboCheck, DubboResponse}

import scala.annotation.implicitNotFound

trait DubboCheckSupport {

  def simple: DubboSimpleCheck.type = DubboSimpleCheck

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for Dubbo.")
  implicit def checkBuilder2DubboCheck[A, P, X, V](checkBuilder: CheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, DubboCheck[V], DubboResponse[V], P]): DubboCheck[V] =
    checkBuilder.build(materializer)

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for Dubbo.")
  implicit def validatorCheckBuilder2DubboCheck[A, P, X, V](validatorCheckBuilder: ValidatorCheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, DubboCheck[V], DubboResponse[V], P]): DubboCheck[V] =
    validatorCheckBuilder.exists

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for Dubbo.")
  implicit def findCheckBuilder2DubboCheck[A, P, X, V](findCheckBuilder: FindCheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, DubboCheck[V], DubboResponse[V], P]): DubboCheck[V] =
    findCheckBuilder.find.exists

  implicit def dubboJsonPathCheckMaterializer[V](implicit jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, DubboCheck[V], DubboResponse[V], JsonNode] = new DubboJsonPathCheckMaterializer[V](jsonParsers)
}
