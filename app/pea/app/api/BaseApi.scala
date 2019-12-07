package pea.app.api

import akka.util.ByteString
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator
import org.pac4j.play.PlayWebContext
import org.pac4j.play.scala.Security
import pea.common.model.ApiRes
import pea.common.util.JsonUtils
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc._

import scala.collection.JavaConverters.asScalaBuffer
import scala.concurrent.{ExecutionContext, Future}

trait BaseApi extends Security[CommonProfile] {

  import BaseApi._

  def getWsProfile(auth: JwtAuthenticator)(implicit request: RequestHeader): CommonProfile = {
    val token = request.getQueryString("token")
    if (token.nonEmpty) {
      auth.validateToken(token.get)
    } else {
      null
    }
  }

  def getProfiles()(implicit request: RequestHeader): List[CommonProfile] = {
    val webContext = new PlayWebContext(request, playSessionStore)
    val profileManager = new ProfileManager[CommonProfile](webContext)
    val profiles = profileManager.getAll(true)
    asScalaBuffer(profiles).toList
  }

  def getProfileId()(implicit request: RequestHeader): String = {
    getProfiles().head.getId
  }

  implicit class JsonToClass(req: Request[ByteString]) {
    def bodyAs[T <: AnyRef](c: Class[T]): T = JsonUtils.parse[T](req.body.decodeString("UTF-8"), c)
  }

  /** convert any success response from service to api Action */
  def toActionResultFromAny(any: Any): Result = {
    OkApiRes(ApiRes(data = any))
  }

  implicit class ServiceResponseToOkResult(f: Future[Any])(implicit ec: ExecutionContext) {
    def toOkResult = f.map(toActionResultFromAny(_))
  }

  def getI18nMessage(key: String, args: Any*)(implicit request: RequestHeader): String = {
    val requestLocal = request.headers.get("Local")
    val langs = controllerComponents.langs
    implicit val lang = if (requestLocal.nonEmpty) {
      langs.availables.find(_.code == requestLocal.get).getOrElse(langs.availables.head)
    } else {
      langs.availables.head
    }
    messagesApi(key, args: _*)
  }
}

object BaseApi {

  object OkApiRes {
    def apply(apiRes: ApiRes): Result = {
      Result(
        header = ResponseHeader(200),
        HttpEntity.Strict(ByteString(JsonUtils.stringify(apiRes)), Some(ContentTypes.JSON))
      )
    }
  }

  /** disable nginx cache for Server Send Event */
  val responseNoCacheHeaders = Seq(
    ("Cache-Control", "no-cache"), ("X-Accel-Buffering", "no")
  )
  val DEFAULT_SOURCE_BUFFER_SIZE = 100
}
