package asura.pea.dubbo

import asura.pea.dubbo.api.GreetingService
import com.alibaba.dubbo.config.{ApplicationConfig, ReferenceConfig}

object GreetingConsumerApp extends RegistryAddressConfig {

  def main(args: Array[String]): Unit = {
    val reference = new ReferenceConfig[GreetingService]()
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
