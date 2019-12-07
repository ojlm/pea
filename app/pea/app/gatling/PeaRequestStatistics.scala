package pea.app.gatling

import pea.app.gatling.PeaRequestStatistics.{PeaGroupedCount, PeaStatistics}

case class PeaRequestStatistics(
                                 name: String,
                                 path: String,
                                 numberOfRequestsStatistics: PeaStatistics[Long],
                                 minResponseTimeStatistics: PeaStatistics[Int],
                                 maxResponseTimeStatistics: PeaStatistics[Int],
                                 meanStatistics: PeaStatistics[Int],
                                 stdDeviationStatistics: PeaStatistics[Int],
                                 percentiles1: PeaStatistics[Int],
                                 percentiles2: PeaStatistics[Int],
                                 percentiles3: PeaStatistics[Int],
                                 percentiles4: PeaStatistics[Int],
                                 groupedCounts: Seq[PeaGroupedCount],
                                 meanNumberOfRequestsPerSecondStatistics: PeaStatistics[Double]
                               )

object PeaRequestStatistics {

  final case class PeaGroupedCount(name: String, count: Long, total: Long) {
    val percentage: Int = if (total == 0) 0 else (count.toDouble / total * 100).round.toInt
  }

  final case class PeaStatistics[T: Numeric](name: String, total: T, success: T, failure: T) {
    def all = List(total, success, failure)
  }

}
