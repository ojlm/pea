package asura.pea.service

import java.io.File
import java.nio.file.{Files, StandardOpenOption}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import asura.common.model.ApiRes
import asura.common.util.JsonUtils
import asura.pea.PeaConfig
import asura.pea.PeaConfig._
import asura.pea.http.HttpClient
import asura.pea.model.{MemberStatus, PeaMember, SingleHttpScenarioMessage}

import scala.collection.mutable
import scala.concurrent.Future

object PeaService {

  def getMemberStatus(member: PeaMember): Future[MemberStatus] = {
    HttpClient.wsClient
      .url(s"${member.toAddress}/api/gatling/status")
      .get()
      .map(response => {
        JsonUtils.parse(response.body[String], classOf[MemberStatus])
      })
  }

  def isWorkersAvailable(workers: Seq[PeaMember]): Future[WorkersAvailable] = {
    val errors = mutable.Map[String, String]()
    val futures = workers.map(member => {
      PeaService.getMemberStatus(member)
        .map(memberStatus =>
          if (MemberStatus.IDLE.equals(memberStatus.status)) {
            (true, null)
          } else {
            errors += (s"${member.toAddress}" -> memberStatus.status)
            (false, memberStatus.status)
          }
        )
        .recover {
          case t: Throwable =>
            errors += (s"${member.toAddress}" -> t.getMessage)
            (false, t.getMessage)
        }
    })
    Future.sequence(futures).map(_ => WorkersAvailable(errors.isEmpty, errors))
  }

  def sendSingleHttpScenario(member: PeaMember, load: SingleHttpScenarioMessage): Future[ApiRes] = {
    HttpClient.wsClient
      .url(s"${member.toAddress}/api/gatling/single")
      .post(JsonUtils.stringify(load))
      .map(response => {
        JsonUtils.parse(response.body[String], classOf[ApiRes])
      })
  }

  def downloadSimulationLog(member: PeaMember, runId: String): Future[File] = {
    HttpClient.wsClient
      .url(s"${member.toAddress}/api/gatling/simulation/${runId}")
      .withMethod("GET").stream()
      .flatMap(res => {
        val file = new File(s"${PeaConfig.resultsFolder}/${runId}/${member.address}.${member.port}.log")
        val os = Files.newOutputStream(file.toPath, StandardOpenOption.CREATE_NEW)
        val sink = Sink.foreach[ByteString] { bytes =>
          os.write(bytes.toArray)
        }
        res.bodyAsSource
          .runWith(sink)
          .andThen {
            case result =>
              os.close()
              result.get
          }
          .map(_ => file)
      })
  }

  // same with the gatling
  def generateRunId(simulationId: String, start: Long): String = {
    simulationId + "-" +
      DateTimeFormatter
        .ofPattern("yyyyMMddHHmmssSSS")
        .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneOffset.UTC))
  }

  case class WorkersAvailable(
                               available: Boolean,
                               errors: mutable.Map[String, String],
                               var runId: String = null
                             )

}
