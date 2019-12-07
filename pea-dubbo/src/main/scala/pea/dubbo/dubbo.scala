package pea

import io.gatling.core.check.Check

package object dubbo {

  case class DubboResponse[V](value: V)

  type DubboCheck[V] = Check[DubboResponse[V]]
}
