package pea.app.compiler

import java.io.File

import pea.app.PeaConfig
import pea.app.actor.CompilerActor.SyncCompileMessage
import pea.common.util.{ProcessUtils, StringUtils, XtermUtils}
import sbt.internal.inc.{AnalysisStore => _, CompilerCache => _}
import xsbti.compile.{FileAnalysisStore => _, ScalaInstance => _}

import scala.concurrent.{ExecutionContext, Future}

object ScalaCompiler {

  private def getFullClasspath(): String = {
    if (StringUtils.isNotEmpty(PeaConfig.compilerExtraClasspath)) {
      s"${System.getProperty("java.class.path")}:${PeaConfig.compilerExtraClasspath}"
    } else {
      System.getProperty("java.class.path")
    }
  }

  val classpath = getFullClasspath()
    .split(File.pathSeparator)
    .filter(p => {
      // filter idea_rt.jar when run in idea ide
      !p.contains("idea_rt.jar")
    })
    .mkString(File.pathSeparator)

  val compiler = ZincCompilerInstance.build(classpath)

  def doCompile(msg: SyncCompileMessage): Future[CompileResponse] = {
    doCompile(CompilerConfiguration.fromCompileMessage(msg))
  }

  def doCompile(config: CompilerConfiguration): Future[CompileResponse] = {
    implicit val ec = ExecutionContext.global
    Future {
      if (null != compiler) {
        compiler.doCompile(config)
      } else {
        val errMsg = "Uninitialized compiler"
        if (null != PeaConfig.compilerMonitorActor) {
          PeaConfig.compilerMonitorActor ! s"${XtermUtils.redWrap("[error]")}[zinc] ${errMsg}"
        }
        CompileResponse(false, errMsg)
      }
    }.recover {
      case t: Throwable => CompileResponse(false, t.getMessage)
    }
  }

  def getGatlingCmd(message: SyncCompileMessage): String = {
    val cmd = s"java -Dfile.encoding=UTF-8 -cp ${classpath} " +
      s"io.gatling.compiler.ZincCompiler " +
      s"-sf ${message.srcFolder} " +
      s"-bf ${message.outputFolder} " +
      s"${if (message.verbose) "-eso -verbose" else ""}"
    cmd
  }

  def doGatlingCompileWithErrors(message: SyncCompileMessage): Future[CompileResponse] = {
    implicit val ec = ExecutionContext.global
    val errors = StringBuilder.newBuilder
    val futureCode = ProcessUtils.execAsync(
      getGatlingCmd(message),
      (_: String) => {},
      (stderr: String) => {
        errors.append(stderr).append("\n")
        ()
      },
      None
    ).get
    futureCode.map(code => {
      CompileResponse(code == 0, errors.toString)
    })
  }

  def doGatlingCompile(
                        message: SyncCompileMessage,
                        stdout: String => Unit = (_) => {},
                        stderr: String => Unit = (_) => {},
                      ): Future[Int] = {
    implicit val ec = ExecutionContext.global
    ProcessUtils.execAsync(getGatlingCmd(message), stdout, stderr, None).get
  }
}
