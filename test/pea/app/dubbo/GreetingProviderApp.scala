package pea.app.dubbo

import java.util.concurrent.CountDownLatch

import org.apache.dubbo.config.{ApplicationConfig, RegistryConfig, ServiceConfig}
import pea.app.dubbo.api.GreetingService
import pea.app.dubbo.provider.GreetingsServiceImpl

object GreetingProviderApp extends RegistryAddressConfig {

  def main(args: Array[String]): Unit = {
    val service = new ServiceConfig[GreetingService]()
    service.setApplication(new ApplicationConfig("pea-dubbo-provider"))
    service.setRegistry(new RegistryConfig(RegistryAddressNA))
    service.setInterface(classOf[GreetingService])
    service.setRef(new GreetingsServiceImpl())
    service.export()
    println(s"${service.getInterface}: ${service.getExportedUrls}")
    new CountDownLatch(1).await()
  }
}
