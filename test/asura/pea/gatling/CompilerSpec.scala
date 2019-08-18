package asura.pea.gatling

import asura.common.util.FutureUtils.RichFuture
import asura.pea.IDEPathHelper
import asura.pea.actor.ZincCompilerActor
import asura.pea.actor.ZincCompilerActor.CompileMessage
import com.typesafe.scalalogging.StrictLogging

object CompilerSpec extends StrictLogging {

  def main(args: Array[String]): Unit = {

    val compileMessage = CompileMessage(
      srcFolder = s"${IDEPathHelper.projectRootDir}/test/simulations",
      outputFolder = s"${IDEPathHelper.projectRootDir}/logs/output",
      verbose = false,
    )
    val code = ZincCompilerActor.doCompile(
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
