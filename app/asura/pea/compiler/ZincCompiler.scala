package asura.pea.compiler

import asura.common.util.ProcessUtils
import asura.pea.actor.ZincCompilerActor.{CompileMessage, CompileResponse}

import scala.concurrent.{ExecutionContext, Future}

object ZincCompiler {

  lazy val currentClassPath = System
    .getProperty("java.class.path")
    .split(":")
    .filter(p => {
      // filter idea jar
      !p.contains("idea_rt.jar")
    })
    .mkString(":")

  def getGatlingCmd(message: CompileMessage): String = {
    val cmd = s"java -Dfile.encoding=UTF-8 -cp ${currentClassPath} " +
      s"io.gatling.compiler.ZincCompiler " +
      s"-sf ${message.srcFolder} " +
      s"-bf ${message.outputFolder} " +
      s"${if (message.verbose) "-eso -verbose" else ""}"
    cmd
  }

  def doGatlingCompileWithErrors(message: CompileMessage): Future[CompileResponse] = {
    implicit val ec = ExecutionContext.global
    val errors = StringBuilder.newBuilder
    val futureCode = ProcessUtils.execAsync(
      getGatlingCmd(message),
      (_: String) => {},
      (stderr: String) => {
        errors.append(stderr).append("\n")
        ()
      }
    ).get
    futureCode.map(code => {
      CompileResponse(code == 0, errors.toString)
    })
  }

  def doGatlingCompile(
                        message: CompileMessage,
                        stdout: String => Unit = (_) => {},
                        stderr: String => Unit = (_) => {},
                      ): Future[Int] = {
    implicit val ec = ExecutionContext.global
    ProcessUtils.execAsync(getGatlingCmd(message), stdout, stderr).get
  }
}
