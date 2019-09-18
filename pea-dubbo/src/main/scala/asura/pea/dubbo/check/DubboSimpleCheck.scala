package asura.pea.dubbo.check

import java.util.{Map => JMap}

import asura.pea.dubbo.{DubboCheck, DubboResponse}
import io.gatling.commons.validation._
import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session

final case class DubboSimpleCheck[R](func: DubboResponse[R] => Boolean) extends DubboCheck[R] {

  override def check(response: DubboResponse[R], session: Session, preparedCache: JMap[Any, Any]): Validation[CheckResult] = {
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
