package pea.app.gatling

import com.typesafe.scalalogging.StrictLogging
import pea.app.IDEPathHelper
import pea.app.actor.CompilerActor.SyncCompileMessage
import pea.app.compiler.ScalaCompiler
import pea.common.util.FutureUtils.RichFuture

object CompilerSpec extends StrictLogging {

  def main(args: Array[String]): Unit = {

    val compileMessage = SyncCompileMessage(
      srcFolder = s"${IDEPathHelper.projectRootDir}/test/simulations",
      outputFolder = s"${IDEPathHelper.projectRootDir}/logs/output",
      verbose = false,
    )
    val code = ScalaCompiler.doGatlingCompile(
      compileMessage,
      stdout => {
        logger.info(s"stdout: ${stdout}")
      },
      stderr => {
        logger.error(s"stderr: ${stderr}")
      }
    ).await
    logger.info(s"exit: ${code}")
  }
}
