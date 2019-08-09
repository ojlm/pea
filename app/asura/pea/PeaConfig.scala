package asura.pea

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.apache.curator.framework.CuratorFramework

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object PeaConfig {

  val DEFAULT_SCHEME = "pea"
  val DEFAULT_WS_ACTOR_BUFFER_SIZE = 10000
  val KEEP_ALIVE_INTERVAL = 2
  val PATH_WORKERS = "workers"
  val PATH_REPORTERS = "reporters"

  implicit val DEFAULT_ACTOR_ASK_TIMEOUT: Timeout = 1 minutes
  implicit var system: ActorSystem = _
  implicit var dispatcher: ExecutionContext = _
  implicit var materializer: ActorMaterializer = _

  var zkClient: CuratorFramework = null
  var zkRootPath: String = null
  var zkCurrNode: String = null
  var zkCurrWorkerPath: String = null
  var zkCurrReporterPath: String = null
  var resultsFolder: String = null
  var workerActor: ActorRef = null
  var reporterActor: ActorRef = null
  var monitorActor: ActorRef = null
  var reportLogoHref: String = null
  var reportDescHref: String = null
  var reportDescContent: String = null
}
