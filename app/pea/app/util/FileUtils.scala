package pea.app.util

import java.io.{File, RandomAccessFile}

object FileUtils {

  def readHead1K(file: File): String = {
    val bytes = Array.fill[Byte](1024)(0)
    val access = new RandomAccessFile(file, "r")
    try {
      if (file.length() <= 1024) {
        access.readFully(bytes, 0, file.length().toInt)
        new String(bytes, 0, file.length().toInt)
      } else {
        access.readFully(bytes, 0, 1024)
        new String(bytes)
      }
    } finally {
      access.close()
    }
  }
}
