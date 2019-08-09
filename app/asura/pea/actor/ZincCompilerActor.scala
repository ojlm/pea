package asura.pea.actor

import akka.actor.Props
import asura.common.actor.BaseActor
import asura.common.util.ProcessUtils
import asura.pea.actor.ZincCompilerActor.CompileMessage

import scala.concurrent.{ExecutionContext, Future}

class ZincCompilerActor extends BaseActor {

  override def receive: Receive = {
    case message: CompileMessage => ZincCompilerActor.doCompile(message)
  }
}

object ZincCompilerActor {

  case class CompileMessage(srcFolder: String, outputFolder: String, verbose: Boolean = false)

  def props() = Props(new ZincCompilerActor())

  lazy val currentClassPath = System
    .getProperty("java.class.path")
    .split(":")
    .filter(p => {
      // filter idea jar
      !p.contains("idea_rt.jar")
    })
    .mkString(":")

  def getCmd(message: CompileMessage): String = {
    val cmd = s"java -Dfile.encoding=UTF-8 -cp ${currentClassPath} " +
      s"io.gatling.compiler.ZincCompiler " +
      s"-sf ${message.srcFolder} " +
      s"-bf ${message.outputFolder} " +
      s"${if (message.verbose) "-eso -verbose" else ""}"
    cmd
  }

  def doCompile(
                 message: CompileMessage,
                 stdout: String => Unit = (_) => {},
                 stderr: String => Unit = (_) => {},
               ): Future[Int] = {
    implicit val ec = ExecutionContext.global
    ProcessUtils.execAsync(getCmd(message), stdout, stderr).get
  }

}
