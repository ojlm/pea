package asura.pea.dubbo.check

import java.util

import asura.pea.dubbo.{DubboCheck, DubboResponse}
import io.gatling.commons.validation.Validation
import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session

case class DubboCheckModel[V](wrapped: DubboCheck[V]) extends DubboCheck[V] {
  override def check(response: DubboResponse[V], session: Session)(implicit preparedCache: util.Map[Any, Any]): Validation[CheckResult] = {
    wrapped.check(response, session)
  }
}
