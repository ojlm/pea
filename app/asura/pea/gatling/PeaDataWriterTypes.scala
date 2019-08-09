package asura.pea.gatling

object PeaDataWriterTypes {

  private val AllTypes = Seq(
    PeaDataWriterType,
    ConsoleDataWriterType,
    FileDataWriterType,
    GraphiteDataWriterType,
    LeakReporterDataWriterType
  ).map(t => t.name -> t).toMap

  def findByName(name: String): Option[DataWriterType] = AllTypes.get(name)

  sealed abstract class DataWriterType(val name: String, val className: String)

  object PeaDataWriterType extends DataWriterType("pea", "asura.pea.gatling.PeaDataWriter")

  object ConsoleDataWriterType extends DataWriterType("console", "io.gatling.core.stats.writer.ConsoleDataWriter")

  object FileDataWriterType extends DataWriterType("file", "io.gatling.core.stats.writer.LogFileDataWriter")

  object GraphiteDataWriterType extends DataWriterType("graphite", "io.gatling.graphite.GraphiteDataWriter")

  object LeakReporterDataWriterType extends DataWriterType("leak", "io.gatling.core.stats.writer.LeakReporterDataWriter")

}
