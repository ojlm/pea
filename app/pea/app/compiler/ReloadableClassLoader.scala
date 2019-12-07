package pea.app.compiler

import java.io.{DataInputStream, File, FileInputStream}

import com.typesafe.scalalogging.Logger
import pea.common.util.LogUtils

class ReloadableClassLoader(
                             parent: ClassLoader,
                             baseDir: String,
                           ) extends ClassLoader(parent) {

  val logger = ReloadableClassLoader.logger

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
          parent.loadClass(name)
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
}
