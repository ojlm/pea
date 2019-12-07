package pea.common.util

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object FutureUtils {

  implicit class RichFuture[T](future: Future[T]) {
    def await(implicit duration: Duration = 600 seconds): T = Await.result(future, duration)
  }

}
