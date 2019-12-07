package pea.common.util

import java.io.InputStream
import java.text.SimpleDateFormat

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object JsonUtils extends JsonUtils {

  val mapper: ObjectMapper with ScalaObjectMapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
  mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
  mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
  mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
  mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)

}

trait JsonUtils {
  val mapper: ObjectMapper

  def stringify(obj: AnyRef): String = {
    mapper.writeValueAsString(obj)
  }

  def parse[T <: AnyRef](content: String, c: Class[T]): T = {
    mapper.readValue(content, c)
  }

  def parse[T <: AnyRef](input: InputStream, c: Class[T]): T = {
    mapper.readValue(input, c)
  }

  def parse[T <: AnyRef](content: String, typeReference: TypeReference[T]): T = {
    mapper.readValue(content, typeReference)
  }
}
