package asura.pea.model

import java.net.URI

import asura.common.util.{LogUtils, StringUtils}
import asura.pea.PeaConfig
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

case class PeaMember(
                      address: String,
                      port: Int,
                      hostname: String,
                    ) {

  def toNodeName: String = PeaMember.toNodeName(address, port, hostname)
}

object PeaMember {

  val logger = Logger("PeaMember")

  def apply(uriWithoutScheme: String): PeaMember = {
    try {
      val uri = URI.create(s"${PeaConfig.DEFAULT_SCHEME}://${uriWithoutScheme}")
      val queryMap = mutable.Map[String, String]()
      uri.getQuery.split("&").foreach(paramStr => {
        val param = paramStr.split("=")
        if (param.length == 2) {
          queryMap += (param(0) -> param(1))
        }
      })
      PeaMember(uri.getHost, uri.getPort, queryMap.getOrElse("hostname", StringUtils.EMPTY))
    } catch {
      case t: Throwable =>
        logger.warn(LogUtils.stackTraceToString(t))
        null
    }
  }

  def toNodeName(address: String, port: Int, hostname: String): String = {
    s"${address}:${port}?hostname=${hostname}"
  }
}
