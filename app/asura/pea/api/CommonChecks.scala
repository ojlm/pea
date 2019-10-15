package asura.pea.api

import asura.pea.PeaConfig
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
}
