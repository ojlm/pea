package asura.pea

import io.gatling.core.check.Check

package object dubbo {

  case class DubboResponse[T](value: T)

  type DubboCheck[T] = Check[DubboResponse[T]]
}
