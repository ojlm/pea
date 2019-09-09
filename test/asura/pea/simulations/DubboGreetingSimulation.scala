package asura.pea.simulations

import asura.pea.dubbo.Predef._
import asura.pea.dubbo.api.GreetingService
import asura.pea.gatling.PeaSimulation
import com.alibaba.dubbo.config.{ApplicationConfig, ReferenceConfig}
import io.gatling.core.Predef._

class DubboGreetingSimulation extends PeaSimulation {
  override val description: String =
    """
      |Dubbo simulation example
      |""".stripMargin

  val scn = scenario("dubbo")
    .exec(
      dubbo(classOf[GreetingService].getName(), f)
        .check(simpleCheck { response =>
          println(s"response: ${response}")
          true
        })
    )

  setUp(
    scn.inject(atOnceUsers(10))
  )

  val reference = new ReferenceConfig[GreetingService]()
  reference.setApplication(new ApplicationConfig("pea-dubbo-consumer"))
  reference.setInterface(classOf[GreetingService])
  reference.setUrl(s"dubbo://127.0.0.1:20880/${classOf[GreetingService].getName}")
  val service = reference.get()

  def f(session: Session): String = {
    service.sayHello("pea")
  }
}
