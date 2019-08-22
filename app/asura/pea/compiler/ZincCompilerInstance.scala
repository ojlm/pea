package asura.pea.compiler

import java.io.File
import java.net.URLClassLoader
import java.nio.file.{Files, Paths}
import java.util.Optional

import asura.common.util.LogUtils
import com.typesafe.scalalogging.{Logger, StrictLogging}
import sbt.internal.inc.classpath.ClasspathUtilities
import sbt.internal.inc.{Locate, LoggedReporter, AnalysisStore => _, CompilerCache => _, _}
import sbt.util.{InterfaceUtil, Level, Logger => SbtLogger}
import xsbti.Problem
import xsbti.compile.{CompileAnalysis, DefinesClass, PerClasspathEntryLookup, FileAnalysisStore => _, ScalaInstance => _, _}

import scala.reflect.io.Directory

class ZincCompilerInstance(
                            classpathFiles: Array[File],
                            compilerBridgeJar: File,
                            scalaInstance: ScalaInstance,
                            cacheFile: File = Paths.get("./inc_compile_cache.zip").toFile,
                          ) extends StrictLogging {

  val analysisStore = AnalysisStore.getCachedStore(FileAnalysisStore.binary(cacheFile))
  val incrementalCompiler = new IncrementalCompilerImpl()

  // sbt logger
  val sbtLogger = new SbtLogger {

    def wrapSbt(log: String) = s"[sbt]:${log}"

    override def trace(t: => Throwable): Unit = {
      logger.debug(wrapSbt(Option(t.getMessage).getOrElse("error")))
    }

    override def success(message: => String): Unit = {
      logger.info(wrapSbt(s"Success: $message"))
    }

    override def log(level: Level.Value, message: => String): Unit =
      level match {
        case Level.Error =>
          if (message.startsWith("## Exception when compiling")) {
            // see IncrementalCompilerImpl.handleCompilationError
            // Exception with stacktrace will be thrown and logged properly below in try/catch block
            logger.error(wrapSbt(message.substring(0, message.indexOf("\n"))))
          } else {
            logger.error(wrapSbt(message))
          }
        case Level.Warn => logger.warn(wrapSbt(message))
        case Level.Info => logger.info(wrapSbt(message))
        case Level.Debug => logger.debug(wrapSbt(message))
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

  val scalaCompiler = new AnalyzingCompiler(
    scalaInstance = scalaInstance,
    provider = ZincCompilerUtil.constantBridgeProvider(scalaInstance, compilerBridgeJar),
    classpathOptions = ClasspathOptionsUtil.auto(),
    onArgsHandler = _ => (),
    classLoaderCache = None
  )
  val compilers = incrementalCompiler.compilers(scalaInstance, ClasspathOptionsUtil.boot(), None, scalaCompiler)
  val setup =
    incrementalCompiler.setup(
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
    Files.createDirectories(config.binariesDirectory)
    val sources: Array[File] = Directory(config.simulationsDirectory.toString)
      .deepFiles
      .collect { case file if file.hasExtension("scala") || file.hasExtension("java") => file.jfile }
      .toArray
    val inputs = incrementalCompiler.inputs(
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
      incrementalCompiler.emptyPreviousResult
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
    val newResult = incrementalCompiler.compile(newInputs, sbtLogger)
    analysisStore.set(AnalysisContents.create(newResult.analysis(), newResult.setup()))
    CompileResponse(true, null, newResult.hasModified)
  }
}

object ZincCompilerInstance {

  val logger = Logger(getClass)

  def build(classpath: String): ZincCompilerInstance = {
    try {
      val classpathFiles: Array[File] = classpath.split(File.pathSeparator).map(new File(_))
      val compilerBridgeJar = jarMatching(classpathFiles, """compiler-bridge_.*\.jar$""")
      val scalaInstance = {
        val scalaLibraryJar: File = jarMatching(classpathFiles, """scala-library-.*\.jar$""")
        val scalaReflectJar: File = jarMatching(classpathFiles, """scala-reflect-.*\.jar$""")
        val scalaCompilerJar: File = jarMatching(classpathFiles, """scala-compiler-.*\.jar$""")
        val allScalaJars: Array[File] = Array(scalaCompilerJar, scalaLibraryJar, scalaReflectJar)
        val scalaVersionExtractor = """scala-library-(.*)\.jar$""".r
        val orgScalaVersionExtractor = """org.scala-lang.scala-library-(.*)\.jar$""".r
        val scalaVersion = scalaLibraryJar.getName match {
          case scalaVersionExtractor(version) => version
          case orgScalaVersionExtractor(version) => version
        }
        new ScalaInstance(
          version = scalaVersion,
          loader = new URLClassLoader(allScalaJars.map(_.toURI.toURL)),
          loaderLibraryOnly = ClasspathUtilities.rootLoader,
          libraryJar = scalaLibraryJar,
          compilerJar = scalaCompilerJar,
          allJars = allScalaJars,
          explicitActual = Some(scalaVersion)
        )
      }
      new ZincCompilerInstance(classpathFiles, compilerBridgeJar, scalaInstance)
    } catch {
      case t: Throwable => logger.error(LogUtils.stackTraceToString(t)); null
    }
  }

  def jarMatching(classpath: Seq[File], regex: String): File =
    classpath
      .find(file => !file.getName.startsWith(".") && regex.r.findFirstMatchIn(file.getName).isDefined)
      .getOrElse(throw new RuntimeException(s"Can't find the jar matching $regex"))
}
