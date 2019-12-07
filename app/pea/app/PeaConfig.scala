package pea.app

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.apache.curator.framework.CuratorFramework
import pea.common.util.StringUtils

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PeaConfig {

  val DEFAULT_SCHEME = "pea"
  val DEFAULT_WS_ACTOR_BUFFER_SIZE = 10000
  val KEEP_ALIVE_INTERVAL = 2
  val PATH_WORKERS = "workers"
  val PATH_REPORTERS = "reporters"
  val PATH_JOBS = "jobs"
  val SIMULATION_LOG_FILE = "simulation.log"

  implicit val DEFAULT_ACTOR_ASK_TIMEOUT: Timeout = 10 minutes
  implicit var system: ActorSystem = _
  implicit var dispatcher: ExecutionContext = _
  implicit var materializer: ActorMaterializer = _

  // system actor
  var workerActor: ActorRef = null
  var reporterActor: ActorRef = null
  var workerMonitorActor: ActorRef = null
  var compilerMonitorActor: ActorRef = null
  var responseMonitorActor: ActorRef = null

  // node
  var address = StringUtils.EMPTY
  var port = 0
  var hostname = StringUtils.EMPTY

  // roles
  var enableReporter = false
  var enableWorker = false

  // zk
  var zkClient: CuratorFramework = null
  var zkRootPath: String = null
  var zkCurrNode: String = null
  var zkCurrWorkerPath: String = null
  var zkCurrReporterPath: String = null

  // gatling report
  var reportLogoHref: String = null
  var reportDescHref: String = null
  var reportDescContent: String = null
  var resultsFolder: String = null
  var resourcesFolder: String = null

  // worker
  var workerProtocol: String = "http"
  var defaultSimulationSourceFolder: String = null
  var defaultSimulationOutputFolder: String = null
  var webSimulationEditorBaseUrl: String = null
  var compilerExtraClasspath: String = null
}
