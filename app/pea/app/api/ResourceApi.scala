package pea.app.api

import java.io.File
import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging
import controllers.Assets
import javax.inject.{Inject, Singleton}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.pac4j.play.scala.SecurityComponents
import pea.app.PeaConfig
import pea.app.api.BaseApi.OkApiRes
import pea.app.model.DownloadResourceRequest
import pea.app.model.ResourceModels.{NewFolder, ResourceCheckRequest, ResourceInfo}
import pea.app.service.ResourceService
import pea.app.util.{FileUtils => PeaFileUtils}
import pea.common.model.{ApiRes, ApiResError}
import pea.common.util.StringUtils
import play.api.http.HttpErrorHandler
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MultipartFormData, Result}

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

  def readJar1k(path: String) = Action {
    checkUserDataFolder {
      read1KRes(path, PeaConfig.compilerExtraClasspath)
    }
  }

  def downloadJar(path: String) = Action {
    checkJarFolder {
      downloadRes(path, PeaConfig.compilerExtraClasspath)
    }
  }

  def uploadJar(path: String) = Action(parse.multipartFormData) { request =>
    checkJarFolder {
      request.body
        .file("file")
        .map(file => uploadRes(file, path, PeaConfig.compilerExtraClasspath))
        .getOrElse(OkApiRes(ApiResError("Missing file")))
    }
  }

  def listJar() = Action(parse.byteString) { implicit req =>
    checkJarFolder {
      listRes(req.bodyAs(classOf[ResourceCheckRequest]), PeaConfig.compilerExtraClasspath)
    }
  }

  def removeJar() = Action(parse.byteString) { implicit req =>
    checkJarFolder {
      removeRes(req.bodyAs(classOf[ResourceCheckRequest]), PeaConfig.compilerExtraClasspath)
    }
  }

  def readResource1k(path: String) = Action {
    checkUserDataFolder {
      read1KRes(path, PeaConfig.resourcesFolder)
    }
  }

  def downloadResource(path: String) = Action {
    checkUserDataFolder {
      downloadRes(path, PeaConfig.resourcesFolder)
    }
  }

  def uploadResource(path: String) = Action(parse.multipartFormData) { request =>
    checkUserDataFolder {
      request.body
        .file("file")
        .map(file => uploadRes(file, path, PeaConfig.resourcesFolder))
        .getOrElse(OkApiRes(ApiResError("Missing file")))
    }
  }

  def listResource() = Action(parse.byteString) { implicit req =>
    checkUserDataFolder {
      listRes(req.bodyAs(classOf[ResourceCheckRequest]), PeaConfig.resourcesFolder)
    }
  }

  def removeResource() = Action(parse.byteString) { implicit req =>
    checkUserDataFolder {
      removeRes(req.bodyAs(classOf[ResourceCheckRequest]), PeaConfig.resourcesFolder)
    }
  }

  def newResourceFolder() = Action(parse.byteString) { implicit req =>
    checkUserDataFolder {
      newResFolder(req.bodyAs(classOf[NewFolder]), PeaConfig.resourcesFolder)
    }
  }

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

  def downloadResourceFrom() = Action(parse.byteString).async { implicit req =>
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

  private def newResFolder(request: NewFolder, baseFolder: String): Result = {
    if (StringUtils.isNotEmpty(request.name)) {
      val subPath = if (StringUtils.isNotEmpty(request.path)) s"${request.path}${File.separator}" else StringUtils.EMPTY
      val absolutePath = s"${baseFolder}${File.separator}${subPath}${request.name}"
      val file = new File(absolutePath)
      if (!file.exists()) {
        if (file.getCanonicalPath.startsWith(baseFolder)) {
          OkApiRes(ApiRes(data = Files.createDirectories(file.toPath).toString))
        } else {
          blockingResult(file)
        }
      } else {
        OkApiRes(ApiResError("Folder already exists"))
      }
    } else {
      OkApiRes(ApiResError("Empty folder name"))
    }
  }

  private def read1KRes(path: String, baseFolder: String): Result = {
    val absolutePath = s"${baseFolder}${File.separator}${path}"
    val file = new File(absolutePath)
    if (file.exists() && file.isFile && file.getCanonicalPath.startsWith(baseFolder)) {
      OkApiRes(ApiRes(data = PeaFileUtils.readHead1K(file)))
    } else {
      blockingResult(file)
    }
  }

  private def downloadRes(path: String, baseFolder: String): Result = {
    val absolutePath = s"${baseFolder}${File.separator}${path}"
    val file = new File(absolutePath)
    if (file.exists() && file.isFile && file.getCanonicalPath.startsWith(baseFolder)) {
      Ok.sendFile(file, false)
    } else {
      blockingResult(file)
    }
  }

  private def uploadRes(upFile: MultipartFormData.FilePart[TemporaryFile], path: String, baseFolder: String): Result = {
    val subPath = if (StringUtils.isNotEmpty(path)) s"${path}${File.separator}" else StringUtils.EMPTY
    val absolutePath = s"${baseFolder}${File.separator}${subPath}${upFile.filename}"
    val targetFile = new File(absolutePath)
    if (targetFile.getCanonicalPath.startsWith(baseFolder)) {
      upFile.ref.moveFileTo(targetFile.toPath, replace = true)
      OkApiRes(ApiRes())
    } else {
      blockingResult(targetFile)
    }
  }

  private def removeRes(check: ResourceCheckRequest, baseFolder: String): Result = {
    val absolutePath = s"${baseFolder}${File.separator}${check.file}"
    val file = new File(absolutePath)
    if (StringUtils.isNotEmpty(check.file) && file.getCanonicalPath.startsWith(baseFolder)) {
      FileUtils.forceDelete(file)
      OkApiRes(ApiRes())
    } else {
      blockingResult(file)
    }
  }

  private def listRes(check: ResourceCheckRequest, baseFolder: String): Result = {
    val absolutePath = s"${baseFolder}${File.separator}${check.file}"
    val file = new File(absolutePath)
    if (file.getCanonicalPath.startsWith(baseFolder)) {
      if (!file.exists()) {
        OkApiRes(ApiRes(data = Nil))
      } else {
        if (file.isDirectory()) {
          val resources = file.listFiles().sortWith((a, b) => {
            if (a.isDirectory && b.isDirectory || a.isFile && b.isFile) {
              a.lastModified() > b.lastModified()
            } else {
              a.isDirectory
            }
          }).map(f => {
            val md5 = if (f.isDirectory) null else DigestUtils.md5Hex(Files.newInputStream(f.toPath))
            ResourceInfo(true, f.isDirectory, f.length, f.lastModified, md5, f.getName)
          })
          OkApiRes(ApiRes(data = resources))
        } else {
          val md5 = DigestUtils.md5Hex(Files.newInputStream(file.toPath))
          OkApiRes(ApiRes(data = Seq(ResourceInfo(true, file.isDirectory, file.length, file.lastModified, md5, file.getName))))
        }
      }
    } else {
      blockingResult(file)
    }
  }

  private def blockingResult(file: File) = {
    OkApiRes(ApiResError(s"Blocking access to this file: ${file.getCanonicalPath}"))
  }
}
