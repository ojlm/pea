package asura.pea.api

import java.io.File
import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.Materializer
import asura.common.model.ApiRes
import asura.common.util.StringUtils
import asura.pea.PeaConfig
import asura.pea.model.DownloadResourceRequest
import asura.pea.model.ResourceModels.{ResourceCheckRequest, ResourceInfo}
import asura.pea.service.ResourceService
import asura.play.api.BaseApi
import asura.play.api.BaseApi.OkApiRes
import com.typesafe.scalalogging.StrictLogging
import controllers.Assets
import javax.inject.{Inject, Singleton}
import org.apache.commons.codec.digest.DigestUtils
import org.pac4j.play.scala.SecurityComponents
import play.api.http.HttpErrorHandler

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResourceApi @Inject()(
                             implicit val system: ActorSystem,
                             implicit val exec: ExecutionContext,
                             implicit val mat: Materializer,
                             val controllerComponents: SecurityComponents,
                             val assets: Assets,
                             val errorHandler: HttpErrorHandler,
                           ) extends BaseApi with CommonChecks with StrictLogging {

  def checkResource() = Action(parse.byteString).async { implicit req =>
    checkWorkerEnable {
      val request = req.bodyAs(classOf[ResourceCheckRequest])
      val file = new File(s"${PeaConfig.resourcesFolder}${File.separator}${request.file}")
      val info = if (file.exists()) {
        val md5 = DigestUtils.md5Hex(Files.newInputStream(file.toPath))
        ResourceInfo(true, file.isDirectory, file.length, file.lastModified, md5)
      } else {
        ResourceInfo(false, false)
      }
      Future.successful(info).toOkResult
    }
  }

  def downloadResource() = Action(parse.byteString).async { implicit req =>
    checkWorkerEnable {
      val request = req.bodyAs(classOf[DownloadResourceRequest])
      if (StringUtils.isNotEmpty(request.file) && StringUtils.isNotEmpty(request.url)) {
        ResourceService.downloadResource(request).map(file => {
          OkApiRes(ApiRes(data = file.getName))
        })
      } else {
        Future.successful(ErrorResult("Illegal request parameters"))
      }
    }
  }
}
