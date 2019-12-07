package pea.dubbo.request

import java.util.concurrent.ConcurrentHashMap

import org.apache.dubbo.common.utils.StringUtils
import org.apache.dubbo.config.{ApplicationConfig, ReferenceConfig, RegistryConfig}
import pea.dubbo.protocol.DubboProtocol

object ReferenceConfigCache {

  private val cache = new ConcurrentHashMap[String, ReferenceConfig[_]]()

  def get[T](clazz: Class[T], protocol: DubboProtocol, actionProtocol: Option[DubboProtocol]): T = {
    val reference = new ReferenceConfig[T]()
    var endpoint: String = null
    if (actionProtocol.nonEmpty) {
      val fallback = actionProtocol.get
      reference.setApplication(new ApplicationConfig(fallback.application.getOrElse(protocol.application.get)))
      if (fallback.registryUrl.nonEmpty || protocol.registryUrl.nonEmpty) {
        reference.setRegistry(new RegistryConfig(fallback.registryUrl.getOrElse(protocol.registryUrl.get)))
      } else {
        endpoint = fallback.endpointUrl.getOrElse(protocol.endpointUrl.get)
        reference.setUrl(s"${endpoint}${clazz.getName}")
      }
      if (fallback.group.nonEmpty || protocol.group.nonEmpty) {
        reference.setGroup(fallback.group.getOrElse(protocol.group.get))
      }
      if (fallback.version.nonEmpty || protocol.version.nonEmpty) {
        reference.setVersion(fallback.version.getOrElse(protocol.version.get))
      }
    } else {
      reference.setApplication(new ApplicationConfig(protocol.application.get))
      if (protocol.registryUrl.nonEmpty) {
        reference.setRegistry(new RegistryConfig(protocol.registryUrl.get))
      } else {
        endpoint = protocol.endpointUrl.get
        reference.setUrl(s"${endpoint}${clazz.getName}")
      }
      if (protocol.group.nonEmpty) reference.setGroup(protocol.group.get)
      if (protocol.version.nonEmpty) reference.setVersion(protocol.version.get)
    }
    reference.setInterface(clazz)
    val key = generateCacheKey(reference, endpoint)
    if (cache.contains(key)) {
      cache.get(key).get().asInstanceOf[T]
    } else {
      val service = reference.get()
      if (null != service) {
        cache.put(generateCacheKey(reference, endpoint), reference)
      }
      service
    }
  }

  // called when gatling ActorSystem terminated
  def clear(): Unit = {
    cache.values().forEach(_.destroy())
    cache.clear()
  }

  /**
    * reference to [[org.apache.dubbo.config.utils.ReferenceConfigCache.DEFAULT_KEY_GENERATOR]]
    * key example: "group1/com.alibaba.foo.FooService:1.0.0@127.0.0.1:20880".
    */
  private def generateCacheKey(reference: ReferenceConfig[_], endpoint: String): String = {
    val sb = StringBuilder.newBuilder
    val group = reference.getGroup()
    if (StringUtils.isNotEmpty(group)) {
      sb.append(group).append("/")
    }
    val interface = reference.getInterface()
    if (StringUtils.isNotEmpty(interface)) {
      sb.append(interface)
    }
    val version = reference.getVersion
    if (StringUtils.isNotEmpty(version)) {
      sb.append(":").append(version)
    }
    if (StringUtils.isNotEmpty(endpoint)) {
      sb.append("@").append(endpoint)
    }
    sb.toString()
  }
}
