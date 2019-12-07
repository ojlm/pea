package pea.app.compiler

import java.io.File
import java.net.URLClassLoader
import java.nio.file.{Files, Paths}
import java.util.Optional

import com.typesafe.scalalogging.{Logger, StrictLogging}
import pea.app.PeaConfig
import pea.common.util.{LogUtils, XtermUtils}
import sbt.internal.inc.classpath.ClasspathUtilities
import sbt.internal.inc.{Locate, LoggedReporter, AnalysisStore => _, CompilerCache => _, _}
import sbt.util.{Level, Logger => SbtLogger}
import xsbti.compile.{CompileAnalysis, DefinesClass, PerClasspathEntryLookup, FileAnalysisStore => _, ScalaInstance => _, _}

import scala.reflect.io.Directory

class ZincCompilerInstance(
                            classpathFiles: Array[File],
                            compilerBridgeJar: File,
                            scalaInstance: ScalaInstance,
                            cacheFile: File = Paths.get("./inc_compile_cache.zip").toFile,
                          ) extends StrictLogging {

  val analysisStore = AnalysisStore.getCachedStore(FileAnalysisStore.binary(cacheFile))
  val compiler = ZincUtil.defaultIncrementalCompiler

  // sbt logger
  val sbtLogger = new SbtLogger {

    def wrapSbt(log: String) = s"[sbt]:${log}"

    override def trace(t: => Throwable): Unit = {
      logger.trace(wrapSbt(Option(t.getMessage).getOrElse("error")))
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
          if (null != PeaConfig.compilerMonitorActor) {
            PeaConfig.compilerMonitorActor ! s"${XtermUtils.redWrap("[error]")}[zinc] ${message}"
          }
        case Level.Warn =>
          logger.warn(wrapSbt(message))
          if (null != PeaConfig.compilerMonitorActor) {
            PeaConfig.compilerMonitorActor ! s"${XtermUtils.yellowWrap("[warn ]")}[zinc] ${message}"
          }
        case Level.Info =>
          logger.info(wrapSbt(message))
          if (null != PeaConfig.compilerMonitorActor) {
            PeaConfig.compilerMonitorActor ! s"${XtermUtils.greenWrap("[info ]")}[zinc] ${message}"
          }
        case Level.Debug =>
          if ((message.startsWith("Scala compilation took") || message.startsWith("No changes"))
            && null != PeaConfig.compilerMonitorActor) {
            PeaConfig.compilerMonitorActor ! s"${XtermUtils.greenWrap("[info ]")}[zinc] ${message}"
          }
          logger.debug(wrapSbt(message))
      }
  }

  val lookup = new PerClasspathEntryLookup {
    override def analysis(classpathEntry: File): Optional[CompileAnalysis] = Optional.empty[CompileAnalysis]

    override def definesClass(classpathEntry: File): DefinesClass = Locate.definesClass(classpathEntry)
  }

  val maxErrors = 100

  val reporter = new LoggedReporter(maxErrors, sbtLogger)

  val scalaCompiler = new AnalyzingCompiler(
    scalaInstance = scalaInstance,
    provider = ZincCompilerUtil.constantBridgeProvider(scalaInstance, compilerBridgeJar),
    classpathOptions = ClasspathOptionsUtil.auto(),
    onArgsHandler = _ => (),
    classLoaderCache = None
  )
  val compilers = ZincUtil.compilers(scalaInstance, ClasspathOptionsUtil.boot(), None, scalaCompiler)
  val setup =
    Setup.of(
      lookup, // lookup
      false, // skip
      cacheFile, // cacheFile
      CompilerCache.fresh, // cache
      IncOptions.of(), // incOptions
      reporter, // reporter
      Optional.empty[CompileProgress], // optionProgress
      Array.empty // extra
    )

  def doCompile(config: CompilerConfiguration): CompileResponse = {
    reporter.reset()
    Files.createDirectories(config.binariesDirectory)
    val sources: Array[File] = Directory(config.simulationsDirectory.toString)
      .deepFiles
      .collect { case file if file.hasExtension("scala") || file.hasExtension("java") => file.jfile }
      .toArray
    val previousResult = {
      val analysisContents = analysisStore.get
      if (analysisContents.isPresent) {
        val analysisContents0 = analysisContents.get
        val previousAnalysis = analysisContents0.getAnalysis
        val previousSetup = analysisContents0.getMiniSetup
        PreviousResult.of(Optional.of(previousAnalysis), Optional.of(previousSetup))
      } else {
        PreviousResult.of(Optional.empty[CompileAnalysis], Optional.empty[MiniSetup])
      }
    }
    val options = CompileOptions.of(
      classpathFiles :+ config.binariesDirectory.toFile, // classpath
      sources, // sources
      config.binariesDirectory.toFile, // classesDirectory
      Array(
        "-encoding",
        config.encoding,
        "-target:jvm-1.8",
        "-deprecation",
        "-feature",
        "-unchecked",
        "-language:implicitConversions",
        "-language:postfixOps"
      ) ++ config.extraScalacOptions, // scalacOptions
      Array.empty, // javacOptions
      maxErrors, // maxErrors
      identity, // sourcePositionMappers
      CompileOrder.Mixed, // order
      Optional.empty[File] // temporaryClassesDirectory
    )
    val inputs = Inputs.of(compilers, options, setup, previousResult)
    val newResult = compiler.compile(inputs, sbtLogger)
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
