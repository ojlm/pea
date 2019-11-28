package asura.pea.model

import asura.common.util.StringUtils

case class SingleRequest(
                          var name: String,
                          var url: String,
                          var method: String,
                          var headers: Map[String, String],
                          var body: String,
                          var virtualhost: String,
                          var params: Map[String, String],
                        ) {

  def getHeaders(): Map[String, String] = {
    if (null == headers) Map.empty else headers
  }

  def getBody(): String = {
    StringUtils.notEmptyElse(body, StringUtils.EMPTY)
  }
  def getVirtualhost(): String = {
    StringUtils.notEmptyElse(virtualhost, StringUtils.EMPTY)
  }
  def getParams(): Map[String, String] = {
    if (null == params) Map.empty else params
  }
}
