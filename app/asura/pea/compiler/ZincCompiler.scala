package asura.pea.compiler

import java.io.File
import java.net.URLClassLoader
import java.nio.file.{Files, Paths}
import java.util.Optional

import asura.common.util.ProcessUtils
import asura.pea.actor.ZincCompilerActor.CompileMessage
import com.typesafe.scalalogging.Logger
import sbt.internal.inc.classpath.ClasspathUtilities
import sbt.internal.inc.{AnalysisStore => _, CompilerCache => _, _}
import sbt.util.{InterfaceUtil, Level, Logger => SbtLogger}
import xsbti.Problem
import xsbti.compile.{FileAnalysisStore => _, ScalaInstance => _, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.io.Directory

object ZincCompiler {

  val logger = Logger(getClass)
  val classpath = System
    .getProperty("java.class.path")
    .split(File.pathSeparator)
    .filter(p => {
      // filter idea jar
      !p.contains("idea_rt.jar")
    })
    .mkString(File.pathSeparator)

  val classpathFiles: Array[File] = classpath.split(File.pathSeparator).map(new File(_))
  val scalaLibraryJar: File = jarMatching(classpathFiles, """scala-library-.*\.jar$""")
  val scalaReflectJar: File = jarMatching(classpathFiles, """scala-reflect-.*\.jar$""")
  val scalaCompilerJar: File = jarMatching(classpathFiles, """scala-compiler-.*\.jar$""")
  val allScalaJars: Array[File] = Array(scalaCompilerJar, scalaLibraryJar, scalaReflectJar)

  val compilerBridgeJar = jarMatching(classpathFiles, """compiler-bridge_.*\.jar$""")
  val cacheFile = Paths.get("./inc_compile_cache.zip").toFile

  val scalaVersionExtractor = """scala-library-(.*)\.jar$""".r
  val scalaVersion = scalaLibraryJar.getName match {
    case scalaVersionExtractor(version) => version
  }

  val sbtLogger = new SbtLogger {
    override def trace(t: => Throwable): Unit = logger.debug(Option(t.getMessage).getOrElse("error"), t)

    override def success(message: => String): Unit = logger.info(s"Success: $message")

    override def log(level: Level.Value, message: => String): Unit =
      level match {
        case Level.Error =>
          if (message.startsWith("## Exception when compiling")) {
            // see IncrementalCompilerImpl.handleCompilationError
            // Exception with stacktrace will be thrown and logged properly below in try/catch block
            logger.error(message.substring(0, message.indexOf("\n")))
          } else {
            logger.error(message)
          }
        case Level.Warn => logger.warn(message)
        case Level.Info => logger.info(message)
        case Level.Debug => logger.debug(message)
      }
  }

  val lookup = new PerClasspathEntryLookup {
    override def analysis(classpathEntry: File): Optional[CompileAnalysis] = Optional.empty[CompileAnalysis]

    override def definesClass(classpathEntry: File): DefinesClass = Locate.definesClass(classpathEntry)
  }
  val maxErrors = 100

  val reporter = new LoggedReporter(maxErrors, sbtLogger) {
    override protected def logError(problem: Problem): Unit = {
      logger.error(problem.message())
    }

    override protected def logWarning(problem: Problem): Unit = {
      logger.warn(problem.message())
    }

    override protected def logInfo(problem: Problem): Unit = {
      logger.info(problem.message())
    }
  }

  val analysisStore = AnalysisStore.getCachedStore(FileAnalysisStore.binary(cacheFile))

  val scalaInstance =
    new ScalaInstance(
      version = scalaVersion,
      loader = new URLClassLoader(allScalaJars.map(_.toURI.toURL)),
      loaderLibraryOnly = ClasspathUtilities.rootLoader,
      libraryJar = scalaLibraryJar,
      compilerJar = scalaCompilerJar,
      allJars = allScalaJars,
      explicitActual = Some(scalaVersion)
    )
  val compiler = new IncrementalCompilerImpl()
  val scalaCompiler = new AnalyzingCompiler(
    scalaInstance = scalaInstance,
    provider = ZincCompilerUtil.constantBridgeProvider(scalaInstance, compilerBridgeJar),
    classpathOptions = ClasspathOptionsUtil.auto(),
    onArgsHandler = _ => (),
    classLoaderCache = None
  )
  val compilers = compiler.compilers(scalaInstance, ClasspathOptionsUtil.boot(), None, scalaCompiler)
  val setup =
    compiler.setup(
      lookup = lookup,
      skip = false,
      cacheFile = cacheFile,
      cache = CompilerCache.fresh,
      incOptions = IncOptions.of(),
      reporter = reporter,
      optionProgress = None,
      extra = Array.empty
    )

  def doCompile(config: CompilerConfiguration): CompileResponse = {
    try {
      doCompile0(config)
    } catch {
      case t: Throwable => CompileResponse(false, t.getMessage)
    }
  }

  private def doCompile0(config: CompilerConfiguration): CompileResponse = {
    Files.createDirectories(config.binariesDirectory)
    val sources: Array[File] = Directory(config.simulationsDirectory.toString)
      .deepFiles
      .collect { case file if file.hasExtension("scala") || file.hasExtension("java") => file.jfile }
      .toArray
    val inputs = compiler.inputs(
      classpath = classpathFiles :+ config.binariesDirectory.toFile,
      sources = sources,
      classesDirectory = config.binariesDirectory.toFile,
      scalacOptions = Array(
        "-encoding",
        config.encoding,
        "-target:jvm-1.8",
        "-deprecation",
        "-feature",
        "-unchecked",
        "-language:implicitConversions",
        "-language:postfixOps"
      ) ++ config.extraScalacOptions,
      javacOptions = Array.empty,
      maxErrors,
      sourcePositionMappers = Array.empty,
      order = CompileOrder.Mixed,
      compilers,
      setup,
      compiler.emptyPreviousResult
    )
    val newInputs =
      InterfaceUtil.toOption(analysisStore.get()) match {
        case Some(analysisContents) =>
          val previousAnalysis = analysisContents.getAnalysis
          val previousSetup = analysisContents.getMiniSetup
          val previousResult = PreviousResult.of(Optional.of(previousAnalysis), Optional.of(previousSetup))
          inputs.withPreviousResult(previousResult)
        case _ =>
          inputs
      }
    val newResult = compiler.compile(newInputs, sbtLogger)
    analysisStore.set(AnalysisContents.create(newResult.analysis(), newResult.setup()))
    CompileResponse(true, null, newResult.hasModified)
  }

  private def jarMatching(classpath: Seq[File], regex: String): File =
    classpath
      .find(file => !file.getName.startsWith(".") && regex.r.findFirstMatchIn(file.getName).isDefined)
      .getOrElse(throw new RuntimeException(s"Can't find the jar matching $regex"))

  def getGatlingCmd(message: CompileMessage): String = {
    val cmd = s"java -Dfile.encoding=UTF-8 -cp ${classpath} " +
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
