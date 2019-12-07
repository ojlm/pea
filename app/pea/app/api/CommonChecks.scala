package pea.app.api

import pea.app.PeaConfig
import pea.common.util.StringUtils
import play.api.mvc.Result

import scala.concurrent.Future

trait CommonChecks extends CommonFunctions {

  def checkWorkerEnable(func: => Future[Result]): Future[Result] = {
    if (PeaConfig.enableWorker) {
      func
    } else {
      FutureErrorResult("Role worker is disabled")
    }
  }

  def checkReporterEnable(func: => Future[Result]): Future[Result] = {
    if (PeaConfig.enableReporter) {
      func
    } else {
      FutureErrorResult("Role reporter is disabled")
    }
  }

  def checkUserDataFolder(func: => Result): Result = {
    if (StringUtils.isNotEmpty(PeaConfig.resourcesFolder)) {
      func
    } else {
      ErrorResult("Config 'resources' is not set")
    }
  }

  def checkJarFolder(func: => Result): Result = {
    if (StringUtils.isNotEmpty(PeaConfig.compilerExtraClasspath)) {
      func
    } else {
      ErrorResult("Config 'classpath' is not set")
    }
  }
}
