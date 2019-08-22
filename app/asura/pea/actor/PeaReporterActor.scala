package asura.pea.actor

import akka.actor.Props
import akka.pattern.pipe
import asura.common.actor.BaseActor
import asura.pea.PeaConfig
import asura.pea.actor.PeaReporterActor.{RunSimulationJob, SingleHttpScenarioJob}
import asura.pea.model._
import asura.pea.service.PeaService
import asura.pea.service.PeaService.WorkersAvailable

import scala.concurrent.Future

class PeaReporterActor extends BaseActor {

  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case SingleHttpScenarioJob(workers, request) =>
      checkAndStartJob(workers, request) pipeTo sender()
    case RunSimulationJob(workers, request) =>
      checkAndStartJob(workers, request) pipeTo sender()
    case _ =>
  }

  private def checkAndStartJob(
                                workers: Seq[PeaMember],
                                message: LoadMessage
                              ): Future[WorkersAvailable] = {
    PeaService.isWorkersAvailable(workers)
      .map(res => {
        if (res.available) {
          val start = System.currentTimeMillis()
          message.simulationId = PeaConfig.hostname
          message.start = start
          res.runId = PeaService.generateRunId(PeaConfig.hostname, start)
          context.actorOf(ReporterWorkersActor.props(workers), res.runId) ! message
        }
        res
      })
  }
}

object PeaReporterActor {

  def props() = Props(new PeaReporterActor())

  case class SingleHttpScenarioJob(
                                    workers: Seq[PeaMember],
                                    request: SingleHttpScenarioMessage,
                                  ) extends LoadJob

  case class RunSimulationJob(
                               workers: Seq[PeaMember],
                               request: RunSimulationMessage,
                             ) extends LoadJob

}
