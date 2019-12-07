package pea.app.simulations

import io.gatling.core.Predef._
import pea.app.dubbo.api.GreetingService
import pea.app.gatling.PeaSimulation
import pea.dubbo.Predef._

class DubboGreetingSimulation extends PeaSimulation {

  override val description: String =
    """
      |Dubbo simulation example
      |""".stripMargin

  val dubboProtocol = dubbo
    .application("gatling-pea")
    .endpoint("127.0.0.1", 20880)
    .threads(10)

  val scn = scenario("dubbo")
    .exec(
      invoke(classOf[GreetingService]) { (service, _) =>
        service.sayHello("pea")
      }.check(simple { response =>
        response.value == "hi, pea"
      }).check(
        jsonPath("$").is("hi, pea")
      )
    )

  setUp(
    scn.inject(atOnceUsers(10000))
  ).protocols(dubboProtocol)
}
