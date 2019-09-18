package asura.pea

import io.gatling.core.check.Check

package object grpc {

  case class GrpcResponse[V](value: V)

  type GrpcCheck[V] = Check[GrpcResponse[V]]

}
