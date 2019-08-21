package asura.pea.compiler

import asura.pea.IDEPathHelper
import asura.pea.actor.ZincCompilerActor.CompileMessage

object ZincCompilerSpec {

  def main(args: Array[String]): Unit = {
    val config = CompilerConfiguration.fromCompileMessage(CompileMessage(
      s"${IDEPathHelper.projectRootDir}/test/simulations",
      s"${IDEPathHelper.projectRootDir}/logs/output"
    ))
    val result = ZincCompiler.doCompile(config)
    println(result)
  }
}
