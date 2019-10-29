package asura.pea.hook

import java.io.File
import java.net.{InetAddress, NetworkInterface, URL, URLClassLoader}
import java.util

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.ActorMaterializer
import asura.common.util.{LogUtils, StringUtils}
import asura.pea.PeaConfig
import asura.pea.actor.CompilerActor.SyncCompileMessage
import asura.pea.actor.WorkerActor.WatchSelf
import asura.pea.actor._
import asura.pea.compiler.CompileResponse
import asura.pea.http.HttpClient
import asura.pea.model.PeaMember
import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Singleton}
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.api.ACLProvider
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

  implicit val ec = system.dispatcher
  implicit val askTimeout = PeaConfig.DEFAULT_ACTOR_ASK_TIMEOUT

  PeaConfig.system = system
  PeaConfig.dispatcher = system.dispatcher
  PeaConfig.materializer = ActorMaterializer()(system)
  PeaConfig.resultsFolder = configuration.get[String]("pea.results.folder")
  PeaConfig.reportLogoHref = getStringFromConfig("pea.results.report.logo.href")
  PeaConfig.reportDescHref = getStringFromConfig("pea.results.report.desc.href")
  PeaConfig.reportDescContent = getStringFromConfig("pea.results.report.desc.content")
  PeaConfig.defaultSimulationSourceFolder = getStringFromConfig("pea.worker.source")
  PeaConfig.defaultSimulationOutputFolder = getStringFromConfig("pea.worker.output")
  PeaConfig.resourcesFolder = getStringFromConfig("pea.worker.resources")
  PeaConfig.compilerExtraClasspath = getStringFromConfig("pea.worker.classpath")
  PeaConfig.webSimulationEditorBaseUrl = getStringFromConfig("pea.simulations.webEditorBaseUrl")
  addSimulationOutputToClasspath()
  val enableZk = configuration.getOptional[Boolean]("pea.zk.enabled").getOrElse(false)
  if (enableZk) {
    registerToZK()
  }

  // init global actors
  PeaConfig.reporterActor = system.actorOf(ReporterActor.props())
  PeaConfig.workerActor = system.actorOf(WorkerActor.props())
  PeaConfig.workerMonitorActor = system.actorOf(WorkerMonitorActor.props())
  PeaConfig.compilerMonitorActor = system.actorOf(CompilerMonitorActor.props())
  PeaConfig.responseMonitorActor = system.actorOf(ResponseMonitorActor.props())

  if (enableZk) {
    PeaConfig.workerActor ! WatchSelf
  }

  // compile simulations at startup
  if (configuration.getOptional[Boolean]("pea.simulations.compileAtStartup").getOrElse(false)) {
    (PeaConfig.workerActor ? SyncCompileMessage()).map(res => {
      logger.info(s"Compiler status: ${res.asInstanceOf[CompileResponse]}")
    })
  }

  // add stop hook
  lifecycle.addStopHook { () =>
    Future {
      HttpClient.close()
    }
  }

  def registerToZK(): Unit = {
    val addressOpt = configuration.getOptional[String]("pea.address")
    PeaConfig.address = if (addressOpt.nonEmpty) {
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
    PeaConfig.hostname = try {
      import scala.sys.process._
      "hostname".!!.trim
    } catch {
      case _: Throwable => "Unknown"
    }
    val portOpt = configuration.getOptional[Int]("pea.port")
    PeaConfig.port = portOpt.getOrElse(9000)
    PeaConfig.zkCurrNode = PeaMember.toNodeName(PeaConfig.address, PeaConfig.port, PeaConfig.hostname)
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
    val protocolOpt = configuration.getOptional[String]("pea.worker.protocol")
    if (protocolOpt.nonEmpty) PeaConfig.workerProtocol = protocolOpt.get
    PeaConfig.zkClient = builder.build()
    PeaConfig.zkClient.start()
    try {
      if (configuration.getOptional[Boolean]("pea.zk.role.worker").getOrElse(true)) {
        PeaConfig.enableWorker = true
        PeaConfig.zkCurrWorkerPath = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_WORKERS}/${PeaConfig.zkCurrNode}"
      }
      if (configuration.getOptional[Boolean]("pea.zk.role.reporter").getOrElse(false)) {
        PeaConfig.enableReporter = true
        PeaConfig.zkCurrReporterPath = s"${PeaConfig.zkRootPath}/${PeaConfig.PATH_REPORTERS}/${PeaConfig.zkCurrNode}"
        PeaConfig.zkClient.create()
          .creatingParentsIfNeeded()
          .withMode(CreateMode.EPHEMERAL)
          .forPath(PeaConfig.zkCurrReporterPath, null)
      }
      lifecycle.addStopHook { () =>
        Future {
          PeaConfig.zkClient.close()
        }
      }
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        System.exit(1)
    }
  }

  private def getStringFromConfig(key: String): String = {
    configuration.getOptional[String](key).getOrElse(StringUtils.EMPTY)
  }

  private def addSimulationOutputToClasspath(): Unit = {
    if (StringUtils.isNotEmpty(PeaConfig.defaultSimulationOutputFolder)) {
      try {
        val file = new File(PeaConfig.defaultSimulationOutputFolder)
        val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
        method.setAccessible(true)
        method.invoke(ClassLoader.getSystemClassLoader, file.toURI.toURL)
      } catch {
        case t: Throwable => logger.warn(LogUtils.stackTraceToString(t))
      }
    }
  }
}
