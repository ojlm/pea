package pea.app.service

import java.io.File
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.typesafe.scalalogging.Logger
import pea.app.PeaConfig
import pea.app.PeaConfig._
import pea.app.actor.CompilerActor.AsyncCompileMessage
import pea.app.http.HttpClient
import pea.app.model._
import pea.common.model.{ApiCode, ApiRes}
import pea.common.util.{JsonUtils, LogUtils}

import scala.collection.mutable
import scala.concurrent.Future

object PeaService {

  type LoadFunction = (PeaMember, LoadMessage) => Future[ApiRes]

  val logger = Logger(getClass)

  def getMemberStatus(member: PeaMember): Future[ApiResMemberStatus] = {
    HttpClient.wsClient
      .url(s"${PeaConfig.workerProtocol}://${member.toAddress}/api/gatling/status")
      .get()
      .map(response => {
        JsonUtils.parse(response.body[String], classOf[ApiResMemberStatus])
      })
  }

  def stopWorker(member: PeaMember): Future[MemberApiBoolRes] = {
    HttpClient.wsClient
      .url(s"${PeaConfig.workerProtocol}://${member.toAddress}/api/gatling/stop")
      .get()
      .map(response => {
        JsonUtils.parse(response.body[String], classOf[MemberApiBoolRes])
      })
  }

  def stopWorkers(workers: Seq[PeaMember]): Future[WorkersBoolResponse] = {
    buildWorkersBoolResponse(workers, PeaService.stopWorker)
  }

  def compile(member: PeaMember): Future[MemberApiBoolRes] = {
    HttpClient.wsClient
      .url(s"${PeaConfig.workerProtocol}://${member.toAddress}/api/gatling/compile")
      .post(JsonUtils.stringify(AsyncCompileMessage(pull = true)))
      .map(response => {
        JsonUtils.parse(response.body[String], classOf[MemberApiBoolRes])
      })
  }

  def compileWorkers(workers: Seq[PeaMember]): Future[WorkersBoolResponse] = {
    buildWorkersBoolResponse(workers, PeaService.compile)
  }

  private def buildWorkersBoolResponse(
                                        workers: Seq[PeaMember],
                                        func: PeaMember => Future[MemberApiBoolRes]
                                      ): Future[WorkersBoolResponse] = {
    val errors = mutable.Map[String, String]()
    val futures = workers.map(member => {
      func(member).map(res =>
        if (ApiCode.OK.equals(res.code)) {
          (res.data, null)
        } else {
          errors += (member.toAddress -> res.msg)
          (false, res.msg)
        }
      ).recover {
        case t: Throwable =>
          errors += (member.toAddress -> t.getMessage)
          (false, t.getMessage)
      }
    })
    Future.sequence(futures).map(_ => WorkersBoolResponse(errors.isEmpty, errors))
  }

  def isWorkersAvailable(workers: Seq[PeaMember]): Future[WorkersAvailable] = {
    val errors = mutable.Map[String, String]()
    val futures = workers.map(member => {
      PeaService.getMemberStatus(member)
        .map(res =>
          if (ApiCode.OK.equals(res.code)) {
            val memberStatus = res.data
            if (MemberStatus.WORKER_IDLE.equals(memberStatus.status)) {
              (true, null)
            } else {
              errors += (member.toAddress -> memberStatus.status)
              (false, memberStatus.status)
            }
          } else {
            errors += (member.toAddress -> res.msg)
            (false, res.msg)
          }
        )
        .recover {
          case t: Throwable =>
            errors += (member.toAddress -> t.getMessage)
            (false, t.getMessage)
        }
    })
    Future.sequence(futures).map(_ => WorkersAvailable(errors.isEmpty, errors))
  }

  def sendSingleHttpScenario(member: PeaMember, load: LoadMessage): Future[ApiRes] = {
    HttpClient.wsClient
      .url(s"${PeaConfig.workerProtocol}://${member.toAddress}/api/gatling/single")
      .post(JsonUtils.stringify(load))
      .map(response => {
        JsonUtils.parse(response.body[String], classOf[ApiRes])
      })
  }

  def sendScript(member: PeaMember, load: LoadMessage): Future[ApiRes] = {
    HttpClient.wsClient
      .url(s"${PeaConfig.workerProtocol}://${member.toAddress}/api/gatling/script")
      .post(JsonUtils.stringify(load))
      .map(response => {
        JsonUtils.parse(response.body[String], classOf[ApiRes])
      })
  }

  def sendProgram(member: PeaMember, load: LoadMessage): Future[ApiRes] = {
    HttpClient.wsClient
      .url(s"${PeaConfig.workerProtocol}://${member.toAddress}/api/gatling/program")
      .post(JsonUtils.stringify(load))
      .map(response => {
        JsonUtils.parse(response.body[String], classOf[ApiRes])
      })
  }

  def downloadSimulationLog(member: PeaMember, runId: String): Future[File] = {
    HttpClient.wsClient
      .url(s"${PeaConfig.workerProtocol}://${member.toAddress}/api/gatling/simulation/${runId}")
      .withMethod("GET")
      .stream()
      .flatMap(res => {
        val dir = s"${PeaConfig.resultsFolder}${File.separator}${runId}"
        Files.createDirectories(Paths.get(dir))
        val file = new File(s"${dir}${File.separator}${member.address}.${member.port}.log")
        val os = Files.newOutputStream(file.toPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
        val sink = Sink.foreach[ByteString] { bytes =>
          os.write(bytes.toArray)
        }
        res.bodyAsSource
          .runWith(sink)
          .andThen { case result =>
            os.close()
            result.get
          }
          .map(_ => file)
          .recover { case t: Throwable =>
            logger.warn(LogUtils.stackTraceToString(t))
            null
          }
      })
      .recover { case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        null
      }
  }

  // same with the gatling
  def generateRunId(simulationId: String, start: Long): String = {
    simulationId + "-" +
      DateTimeFormatter
        .ofPattern("yyyyMMddHHmmssSSS")
        .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneOffset.UTC))
  }

  case class ApiResMemberStatus(code: String, msg: String, data: MemberStatus)

  case class WorkersAvailable(
                               available: Boolean,
                               errors: mutable.Map[String, String],
                               var runId: String = null
                             )

  case class MemberApiBoolRes(code: String, msg: String, data: Boolean)

  case class WorkersBoolResponse(
                                  result: Boolean,
                                  errors: mutable.Map[String, String],
                                )

}
