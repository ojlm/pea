package pea.app.compiler

import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}

import pea.app.actor.CompilerActor.SyncCompileMessage

case class CompilerConfiguration(
                                  simulationsDirectory: Path,
                                  binariesDirectory: Path,
                                  encoding: String = StandardCharsets.UTF_8.name(),
                                  extraScalacOptions: Seq[String] = Nil
                                )

object CompilerConfiguration {

  def fromCompileMessage(message: SyncCompileMessage): CompilerConfiguration = {
    CompilerConfiguration(
      Paths.get(message.srcFolder).toAbsolutePath(),
      Paths.get(message.outputFolder).toAbsolutePath(),
    )
  }
}
