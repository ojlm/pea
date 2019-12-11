package pea.app.compiler

import java.io.{DataInputStream, File, FileInputStream}
import java.lang.reflect.Method
import java.net.{URL, URLClassLoader}
import java.security.{AccessController, PrivilegedAction}
import java.util.jar.JarFile

import com.typesafe.scalalogging.Logger
import pea.app.PeaConfig
import pea.common.util.{LogUtils, StringUtils}

class ReloadableClassLoader(
                             parent: ClassLoader,
                             baseDir: String,
                           ) extends ClassLoader(parent) {

  val logger = ReloadableClassLoader.logger
  val extUrlClassLoader = ReloadableClassLoader.getExtJarsUrlClassLoader(parent = parent)

  // https://stackoverflow.com/questions/3216780/problem-reloading-a-jar-using-urlclassloader
  def close(): Unit = {
    ReloadableClassLoader.releaseJars(extUrlClassLoader)
  }

  override def loadClass(name: String): Class[_] = {
    findClass(name)
  }

  override def findClass(name: String): Class[_] = {
    try {
      val loaded = findLoadedClass(name)
      if (null == loaded) {
        val file = new File(s"${baseDir}${File.separator}${name.replaceAll("\\.", File.separator)}.class")
        if (file.exists()) {
          val bytes = loadClassData(file)
          defineClass(name, bytes, 0, bytes.length)
        } else {
          extUrlClassLoader.loadClass(name)
        }
      } else {
        loaded
      }
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        parent.loadClass(name)
    }
  }

  @throws[Exception]
  private def loadClassData(file: File): Array[Byte] = {
    val buff = new Array[Byte](file.length().toInt)
    val fis = new FileInputStream(file)
    val dis = new DataInputStream(fis)
    dis.readFully(buff)
    dis.close()
    buff
  }
}

object ReloadableClassLoader {

  val logger = Logger(getClass)
  val JAR_FILE_FACTORY = getJarFileFactory()
  val GET = getMethodGetByURL()
  val CLOSE = getMethodCloseJarFile()

  def getExtJarsUrlClassLoader(
                                path: String = PeaConfig.compilerExtraClasspath,
                                parent: ClassLoader = Thread.currentThread().getContextClassLoader,
                              ): ReloadableUrlClassLoader = {
    val urls = if (StringUtils.isNotEmpty(path)) {
      new File(path)
        .listFiles(item => item.isFile && item.getName.endsWith(".jar"))
        .map(file => new URL(s"jar:file:${file.getCanonicalPath}!/"))
    } else {
      Array.empty[URL]
    }
    // URLClassLoader.newInstance(urls, parent)
    AccessController.doPrivileged(new PrivilegedAction[ReloadableUrlClassLoader] {
      override def run(): ReloadableUrlClassLoader = ReloadableUrlClassLoader(urls, parent)
    })
  }

  case class ReloadableUrlClassLoader(urls: Array[URL], parent: ClassLoader)
    extends URLClassLoader(urls, parent) {

    override def loadClass(name: String): Class[_] = {
      val sm = System.getSecurityManager
      if (sm != null) {
        val i = name.lastIndexOf('.')
        if (i != -1) sm.checkPackageAccess(name.substring(0, i))
      }
      val loaded = findLoadedClass(name)
      if (null == loaded) {
        try {
          findClass(name)
        } catch {
          case _: Throwable => parent.loadClass(name)
        }
      } else {
        loaded
      }
    }
  }

  def getJarFileFactory(): Object = {
    try {
      val m = Class.forName(
        "sun.net.www.protocol.jar.JarFileFactory",
        true,
        Thread.currentThread().getContextClassLoader
      ).getMethod("getInstance")
      m.setAccessible(true)
      m.invoke(null)
    } catch {
      case t: Throwable =>
        logger.error(LogUtils.stackTraceToString(t))
        null
    }
  }

  def getMethodGetByURL(): Method = {
    if (null != JAR_FILE_FACTORY) {
      try {
        val method = JAR_FILE_FACTORY.getClass.getMethod("get", classOf[URL])
        method.setAccessible(true)
        method
      } catch {
        case t: Throwable =>
          logger.error(LogUtils.stackTraceToString(t))
          null
      }
    } else {
      null
    }
  }

  def getMethodCloseJarFile(): Method = {
    if (null != JAR_FILE_FACTORY) {
      try {
        val method = JAR_FILE_FACTORY.getClass.getMethod("close", classOf[JarFile])
        method.setAccessible(true)
        method
      } catch {
        case t: Throwable =>
          logger.error(LogUtils.stackTraceToString(t))
          null
      }
    } else {
      null
    }
  }

  def releaseJars(classloader: ReloadableUrlClassLoader): Unit = {
    try {
      classloader.close()
      classloader.urls.foreach(url => {
        if (url.getPath.endsWith("jar") || url.getPath.endsWith("zip")) {
          try {
            CLOSE.invoke(JAR_FILE_FACTORY, GET.invoke(JAR_FILE_FACTORY, url))
          } catch {
            case t: Throwable => logger.error(LogUtils.stackTraceToString(t))
          }
        }
      })
    } finally {
      System.gc()
    }
  }
}
