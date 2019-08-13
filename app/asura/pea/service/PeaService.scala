package asura.pea.service

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset, ZonedDateTime}

import asura.common.model.ApiRes
import asura.common.util.JsonUtils
import asura.pea.PeaConfig._
import asura.pea.http.HttpClient
import asura.pea.model.{MemberStatus, PeaMember, SingleHttpScenarioMessage}

import scala.collection.mutable
import scala.concurrent.Future

object PeaService {

  def getMemberStatus(member: PeaMember): Future[MemberStatus] = {
    HttpClient.wsClient
      .url(s"${member.hostname}:${member.port}/api/gatling/status")
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
            errors += (s"${member.address}:${member.port}" -> memberStatus.status)
            (false, memberStatus.status)
          }
        )
        .recover {
          case t: Throwable =>
            errors += (s"${member.address}:${member.port}" -> t.getMessage)
            (false, t.getMessage)
        }
    })
    Future.sequence(futures).map(_ => WorkersAvailable(errors.isEmpty, errors))
  }

  def sendSingleHttpScenario(member: PeaMember, load: SingleHttpScenarioMessage): Future[ApiRes] = {
    HttpClient.wsClient
      .url(s"${member.hostname}:${member.port}/api/gatling/single")
      .post(JsonUtils.stringify(load))
      .map(response => {
        JsonUtils.parse(response.body[String], classOf[ApiRes])
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
