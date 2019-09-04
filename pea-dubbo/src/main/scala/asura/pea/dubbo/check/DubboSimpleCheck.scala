package asura.pea.dubbo.check

import java.util.{Map => JMap}

import asura.pea.dubbo.DubboCheck
import io.gatling.commons.validation._
import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session

final case class DubboSimpleCheck(func: String => Boolean) extends DubboCheck {

  override def check(response: String, session: Session)(implicit preparedCache: JMap[Any, Any]): Validation[CheckResult] = {
    if (func(response)) {
      CheckResult.NoopCheckResultSuccess
    } else {
      DubboSimpleCheck.DubboSimpleCheckFailure
    }
  }
}

object DubboSimpleCheck {

  private val DubboSimpleCheckFailure = "Dubbo check failed".failure
}
