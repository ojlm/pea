package pea.grpc

import com.trueaccord.lenses.{Lens, Mutation, Updatable}
import io.gatling.core.Predef.value2Expression
import io.gatling.core.session.Expression
import pea.grpc.check.GrpcCheckSupport

object Predef extends GrpcDsl with GrpcCheckSupport {

  implicit class ExprLens[A, B](val l: Lens[A, B]) extends AnyVal {
    def :~(e: Expression[B]): Expression[Mutation[A]] = e.map(l := _)
  }

  implicit class ExprUpdatable[A <: Updatable[A]](val e: Expression[A]) extends AnyVal {
    def updateExpr(mEs: (Lens[A, A] => Expression[Mutation[A]])*): Expression[A] = {
      val mutationExprs = mEs.map(_.apply(Lens.unit))
      s =>
        mutationExprs.foldLeft(e(s)) { (aVal, mExpr) =>
          for {
            a <- aVal
            m <- mExpr(s)
          } yield m(a)
        }
    }
  }

  implicit def value2ExprUpdatable[A <: Updatable[A]](e: A): ExprUpdatable[A] = new ExprUpdatable(value2Expression(e))

  implicit class ExpressionZipping[A](val expression: Expression[A]) extends AnyVal {
    def zipWith[B, C](that: Expression[B])(f: (A, B) => C): Expression[C] = { session =>
      expression(session).flatMap(r1 => that(session).map(r2 => f(r1, r2)))
    }
  }

  implicit class SomeWrapper[T](val value: T) extends AnyVal {
    def some: Some[T] = Some(value)
  }

}
