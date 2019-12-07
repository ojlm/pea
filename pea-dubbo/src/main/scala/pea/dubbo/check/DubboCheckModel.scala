package pea.dubbo.check

import java.util

import io.gatling.commons.validation.Validation
import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session
import pea.dubbo.{DubboCheck, DubboResponse}

case class DubboCheckModel[V](wrapped: DubboCheck[V]) extends DubboCheck[V] {
  override def check(response: DubboResponse[V], session: Session, preparedCache: util.Map[Any, Any]): Validation[CheckResult] = {
    wrapped.check(response, session, preparedCache)
  }
}
