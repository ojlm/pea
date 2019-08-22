package asura.pea.compiler

import asura.common.util.FutureUtils.RichFuture
import asura.pea.IDEPathHelper
import asura.pea.actor.CompilerActor.CompileMessage

object ZincCompilerSpec {

  def main(args: Array[String]): Unit = {
    val config = CompilerConfiguration.fromCompileMessage(CompileMessage(
      s"${IDEPathHelper.projectRootDir}/test/simulations",
      s"${IDEPathHelper.projectRootDir}/logs/output",
    ))
    val result = ScalaCompiler.doCompile(config).await
    println(result)
  }
}
