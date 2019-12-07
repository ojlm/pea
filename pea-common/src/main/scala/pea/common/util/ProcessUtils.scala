package pea.common.util

import java.io.File

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.sys.process._
import scala.util.Try

object ProcessUtils extends ProcessUtils {
  type ExitValue = Int
  type Stdout = String
  type Stderr = String

  type ExecResult = (ExitValue, Stdout, Stderr)

  trait AsyncIntResult {
    def map[T](f: Int => T): Future[T]

    def foreach(f: Int => Unit): Unit

    def onComplete[T](pf: Try[Int] => T): Unit

    def cancel: Cancelable

    def isRunning: Boolean

    def get: Future[Int]
  }

  trait AsyncExecResult {
    def map[T](f: ExecResult => T): Future[T]

    def foreach(f: ExecResult => Unit): Unit

    def onComplete[T](pf: Try[ExecResult] => T): Unit

    def cancel: Cancelable

    def isRunning: Boolean

    def get: Future[ExecResult]
  }

  type Cancelable = () => Unit

  case class ExecutionCanceled(msg: String) extends Exception(msg)

}

trait ProcessUtils {

  import ProcessUtils._

  def exec(cmd: Seq[String], fn: String => Unit, cwd: Option[File], extraEnv: (String, String)*): Int = {
    if (null != fn) Process(cmd, cwd, extraEnv: _*).!(ProcessLogger(fn)) else Process(cmd, cwd).!
  }

  def exec(cmd: String, cwd: Option[File], extraEnv: (String, String)*): ExecResult =
    exec(cmd.split(" "), cwd, extraEnv: _*)

  def exec(cmd: Seq[String], cwd: Option[File], extraEnv: (String, String)*): ExecResult = {
    val stdout = new OutputBuffer
    val stderr = new OutputBuffer

    Try {
      val process = Process(cmd, cwd, extraEnv: _*).run(ProcessLogger(stdout.appendLine, stderr.appendLine))
      process.exitValue()
    }.map((_, stdout.get, stderr.get))
      .recover {
        case t => (-1, "", t.getMessage)
      }.get
  }

  def execAsync(
                 cmd: String,
                 cwd: Option[File],
                 extraEnv: (String, String)*,
               )(implicit ec: ExecutionContext): AsyncExecResult =
    execAsync(cmd.split(" "), cwd, extraEnv: _*)

  def execAsync(
                 cmd: Seq[String],
                 cwd: Option[File],
                 extraEnv: (String, String)*,
               )(implicit ec: ExecutionContext): AsyncExecResult = {
    new AsyncExecResult {

      val (fut, cancelable) = runAsync(cmd, cwd)

      override def cancel: Cancelable = cancelable

      override def onComplete[T](pf: (Try[(ExitValue, Stdout, Stderr)]) => T): Unit = fut.onComplete(pf)

      override def foreach(f: ((ExitValue, Stdout, Stderr)) => Unit): Unit = fut.foreach(f)

      override def isRunning: Boolean = !fut.isCompleted

      override def get: Future[(ExitValue, Stdout, Stderr)] = fut

      override def map[T](f: ((ExitValue, Stdout, Stderr)) => T): Future[T] = fut.map(f)
    }
  }

  private def runAsync(
                        cmd: Seq[String],
                        cwd: Option[File],
                        extraEnv: (String, String)*,
                      )(implicit ec: ExecutionContext): (Future[ExecResult], Cancelable) = {
    val p = Promise[ExecResult]

    val stdout = new OutputBuffer
    val stderr = new OutputBuffer

    val process = Process(cmd, cwd, extraEnv: _*).run(ProcessLogger(stdout.appendLine, stderr.appendLine))
    p.tryCompleteWith(Future(process.exitValue).map(c => (c, stdout.get, stderr.get)))

    val cancelFunc = () => {
      p.tryFailure(new ExecutionCanceled(s"Process: '${cmd.mkString(" ")}' canceled"))
      process.destroy()
    }
    (p.future, cancelFunc)
  }

  class OutputBuffer {
    private val sb = new StringBuilder

    def append(s: String): Unit = sb.append(s)

    def appendLine(s: String): Unit = append(s + "\n")

    def get: String = sb.toString
  }

  def execAsync(
                 cmd: String,
                 fout: String => Unit,
                 ferr: String => Unit,
                 cwd: Option[File],
                 extraEnv: (String, String)*,
               )(implicit ec: ExecutionContext): AsyncIntResult = {
    val (fut, cancelable) = execAsync(cmd.split(" "), fout, ferr, cwd, extraEnv: _*)

    new AsyncIntResult {
      override def map[T](f: Int => T): Future[T] = fut.map(f)

      override def foreach(f: Int => Unit): Unit = fut.foreach(f)

      override def onComplete[T](pf: Try[Int] => T): Unit = fut.onComplete(pf)

      override def cancel: Cancelable = cancelable

      override def isRunning: Boolean = !fut.isCompleted

      override def get: Future[Int] = fut
    }
  }

  def execAsync(
                 cmd: Seq[String],
                 fout: String => Unit,
                 ferr: String => Unit,
                 cwd: Option[File],
                 extraEnv: (String, String)*,
               )(implicit ec: ExecutionContext): (Future[Int], Cancelable) = {
    val p = Promise[Int]

    val process = Process(cmd, cwd, extraEnv: _*).run(ProcessLogger(fout, ferr))
    p.tryCompleteWith(Future(process.exitValue))

    val cancelFunc = () => {
      p.tryFailure(new ExecutionCanceled(s"Process: '${cmd.mkString(" ")}' canceled"))
      process.destroy()
    }
    (p.future, cancelFunc)
  }
}
