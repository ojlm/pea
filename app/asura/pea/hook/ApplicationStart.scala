package asura.pea.hook

import java.net.{InetAddress, NetworkInterface}
import java.nio.charset.StandardCharsets
import java.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import asura.common.util.{JsonUtils, LogUtils, StringUtils}
import asura.pea.PeaConfig
import asura.pea.actor.{PeaMonitorActor, PeaReporterActor, PeaWorkerActor}
import asura.pea.model.{MemberStatus, PeaMember}
import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Singleton}
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.api.ACLProvider
import org.apache.curator.framework.recipes.cache.{NodeCache, NodeCacheListener}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.data.ACL
import org.apache.zookeeper.{CreateMode, ZooDefs}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.collection.JavaConverters._
import scala.concurrent.Future

@Singleton
class ApplicationStart @Inject()(
                                  lifecycle: ApplicationLifecycle,
                                  system: ActorSystem,
                                  configuration: Configuration,
                                ) extends StrictLogging {

  PeaConfig.system = system
  PeaConfig.dispatcher = system.dispatcher
  PeaConfig.materializer = ActorMaterializer()(system)
  PeaConfig.resultsFolder = configuration.get[String]("pea.results.folder")
  PeaConfig.reportLogoHref = configuration
    .getOptional[String]("pea.results.report.logo.href")
    .getOrElse(null)
  PeaConfig.reportDescHref = configuration
    .getOptional[String]("pea.results.report.desc.href")
    .getOrElse(null)
  PeaConfig.reportDescContent = configuration
    .getOptional[String]("pea.results.report.desc.content")
    .getOrElse(null)
  PeaConfig.reporterActor = system.actorOf(PeaReporterActor.props())
  PeaConfig.workerActor = system.actorOf(PeaWorkerActor.props())
  PeaConfig.monitorActor = system.actorOf(PeaMonitorActor.props())

  val enableZk = configuration.getOptional[Boolean]("pea.zk.enabled").getOrElse(false)
  if (enableZk) {
    registerToZK()
  }

  // add stop hook
  lifecycle.addStopHook { () =>
    Future {
      if (null != PeaConfig.zkClient) PeaConfig.zkClient.close()
    }(system.dispatcher)
  }

  def registerToZK(): Unit = {
    val addressOpt = configuration.getOptional[String]("pea.address")
    val address = if (addressOpt.nonEmpty) {
      addressOpt.get
    } else {
      val enumeration = NetworkInterface.getNetworkInterfaces.asScala.toSeq
      val ipAddresses = enumeration.flatMap(p =>
        p.getInetAddresses.asScala.toSeq
      )
      val address = ipAddresses.find { address =>
        val host = address.getHostAddress
        host.contains(".") && !address.isLoopbackAddress
      }.getOrElse(InetAddress.getLocalHost)
      address.getHostAddress
    }
    val portOpt = configuration.getOptional[Int]("pea.port")
    val hostname = try {
      import scala.sys.process._
      "hostname".!!.trim
    } catch {
      case _: Throwable => "Unknown"
    }
    val port = portOpt.getOrElse(9000)
    PeaConfig.zkCurrNode = PeaMember.toNodeName(address, port, hostname)
    PeaConfig.zkRootPath = configuration.getOptional[String]("pea.zk.path").get
    val connectString = configuration.get[String]("pea.zk.connectString")
    val builder = CuratorFrameworkFactory.builder()
    builder.connectString(connectString)
      .retryPolicy(new ExponentialBackoffRetry(1000, 10))
    val usernameOpt = configuration.getOptional[String]("pea.zk.username")
    val passwordOpt = configuration.getOptional[String]("pea.zk.password")
    if (usernameOpt.nonEmpty && passwordOpt.nonEmpty
      && StringUtils.isNotEmpty(usernameOpt.get) && StringUtils.isNotEmpty(passwordOpt.get)
    ) {
      builder.authorization("digest", s"${usernameOpt.get}:${passwordOpt.get}".getBytes)
        .aclProvider(new ACLProvider {
          override def getDefaultAcl: util.List[ACL] = ZooDefs.Ids.CREATOR_ALL_ACL

          override def getAclForPath(path: String): util.List[ACL] = ZooDefs.Ids.CREATOR_ALL_ACL
        })
    }
    PeaConfig.zkClient = builder.build()
    PeaConfig.zkClient.start()
    try {
      if (configuration.getOptional[Boolean]("pea.zk.role.worker").getOrElse(true)) {
        PeaConfig.zkCurrWorkerPath = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_WORKERS}/${PeaConfig.zkCurrNode}"
        val nodeData = JsonUtils.stringify(MemberStatus()).getBytes(StandardCharsets.UTF_8)
        PeaConfig.zkClient.create()
          .creatingParentsIfNeeded()
          .withMode(CreateMode.EPHEMERAL)
          .forPath(PeaConfig.zkCurrWorkerPath, nodeData)
        val nodeCache = new NodeCache(PeaConfig.zkClient, PeaConfig.zkCurrWorkerPath)
        nodeCache.start()
        nodeCache.getListenable.addListener(new NodeCacheListener {
          override def nodeChanged(): Unit = {
            val memberStatus = JsonUtils.parse(
              new String(nodeCache.getCurrentData.getData, StandardCharsets.UTF_8),
              classOf[MemberStatus]
            )
            PeaConfig.workerActor ! memberStatus
          }
        })
      }
      if (configuration.getOptional[Boolean]("pea.zk.role.reporter").getOrElse(false)) {
        PeaConfig.zkCurrReporterPath = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_REPORTERS}/${PeaConfig.zkCurrNode}"
        PeaConfig.zkClient.create()
          .creatingParentsIfNeeded()
          .withMode(CreateMode.EPHEMERAL)
          .forPath(PeaConfig.zkCurrReporterPath, null)
      }
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        System.exit(1)
    }
  }
}
