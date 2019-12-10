package pea.app.dubbo

import org.apache.dubbo.config.ApplicationConfig
import pea.app.dubbo.api.GreetingService
import pea.dubbo.request.CustomReferenceConfig

object GreetingConsumerApp extends RegistryAddressConfig {

  def main(args: Array[String]): Unit = {
    val reference = new CustomReferenceConfig[GreetingService]()
    reference.setApplication(new ApplicationConfig("pea-dubbo-consumer"))
    // reference.setVersion("1.0.0")
    reference.setTimeout(3000)
    reference.setInterface(classOf[GreetingService])
    // reference.setRegistry(new RegistryConfig(RegistryAddressZK))
    reference.setUrl(s"dubbo://127.0.0.1:20880/${classOf[GreetingService].getName}")
    val service = reference.get()
    val response = service.sayHello("pea")
    println(s"Got: ${response}")
    reference.destroy()
    System.exit(0)
  }
}
