package pea.app.compiler

import pea.app.IDEPathHelper
import pea.app.actor.CompilerActor.SyncCompileMessage
import pea.common.util.FutureUtils.RichFuture

object ZincCompilerSpec {

  def main(args: Array[String]): Unit = {
    val config = CompilerConfiguration.fromCompileMessage(SyncCompileMessage(
      s"${IDEPathHelper.projectRootDir}/test/simulations",
      s"${IDEPathHelper.projectRootDir}/logs/output",
    ))
    val result = ScalaCompiler.doCompile(config).await
    println(result)
  }
}
