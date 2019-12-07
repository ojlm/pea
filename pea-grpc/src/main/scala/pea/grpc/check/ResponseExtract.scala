package pea.grpc.check

import io.gatling.commons.validation.{FailureWrapper, SuccessWrapper, Validation, safely}
import io.gatling.core.check._
import io.gatling.core.session.{Expression, ExpressionSuccessWrapper}

import scala.util.{Failure, Success, Try}

object ResponseExtract {

  trait ResponseExtractor[T, X] extends Extractor[T, X] {

    override def name: String = "grpcResponse"

    def extract(prepared: T): Option[X]

    override def apply(prepared: T): Validation[Option[X]] = safely() {
      extract(prepared).success
    }
  }

  case class SingleExtractor[T, X](f: T => Option[X]) extends ResponseExtractor[T, X] {
    override def extract(prepared: T): Option[X] = f(prepared)

    override def arity: String = "find"
  }

  def extract[T, X](f: T => Option[X]) = ValidatorCheckBuilder[ResponseExtract, T, X](
    extractor = SingleExtractor(f).expressionSuccess,
    displayActualValue = true,
  )

  def extractMultiple[T, X](f: T => Option[Seq[X]]) = new DefaultMultipleFindCheckBuilder[ResponseExtract, T, X](
    displayActualValue = true,
  ) {
    override def findExtractor(occurrence: Int): Expression[Extractor[T, X]] = new ResponseExtractor[T, X] {
      override def extract(prepared: T): Option[X] = f(prepared).flatMap(s =>
        if (s.isDefinedAt(occurrence)) Some(s(occurrence)) else None
      )

      // Since the arity traits got fused into the CriterionExtractors
      // and our criteria are functions that do not look good in string
      // I have no choice but to write them manually
      override def arity: String = if (occurrence == 0) "find" else s"find($occurrence)"
    }.expressionSuccess

    override def findAllExtractor: Expression[Extractor[T, Seq[X]]] = new ResponseExtractor[T, Seq[X]] {
      override def extract(prepared: T): Option[Seq[X]] = f(prepared)

      override def arity: String = "findAll"
    }.expressionSuccess

    override def countExtractor: Expression[Extractor[T, Int]] = new ResponseExtractor[T, Int] {
      override def extract(prepared: T): Option[Int] = f(prepared).map(_.size)

      override def arity: String = "count"
    }.expressionSuccess
  }

  def materializer[Res] = new CheckMaterializer[ResponseExtract, GrpcCheck[Res], Try[Res], Res](
    specializer = GrpcCheck(_, GrpcCheck.Value)
  ) {
    override protected def preparer: Preparer[Try[Res], Res] = {
      case Success(value) => value.success
      case Failure(exception) => exception.getMessage.failure
    }
  }
}

trait ResponseExtract
