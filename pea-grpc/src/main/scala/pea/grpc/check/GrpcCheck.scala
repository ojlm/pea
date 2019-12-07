package pea.grpc.check

import java.util.{Map => JMap}

import io.gatling.core.check.Check
import io.gatling.core.session.Session
import pea.grpc.check.GrpcCheck.{Scope, Status}

import scala.annotation.unchecked.uncheckedVariance
import scala.util.Try

case class GrpcCheck[-T](wrapped: Check[Try[T]@uncheckedVariance], scope: Scope) extends Check[Try[T]@uncheckedVariance] {
  override def check(response: Try[T], session: Session, cache: JMap[Any, Any]) =
    wrapped.check(response, session, cache)

  def checksStatus = scope == Status
}

object GrpcCheck {

  sealed trait Scope

  case object Status extends Scope

  case object Value extends Scope

}
