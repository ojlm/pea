package asura.pea.service

import asura.common.util.JsonUtils
import asura.pea.PeaConfig._
import asura.pea.http.HttpClient
import asura.pea.model.{MemberStatus, PeaMember}

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

  case class WorkersAvailable(available: Boolean, errors: mutable.Map[String, String])

}
