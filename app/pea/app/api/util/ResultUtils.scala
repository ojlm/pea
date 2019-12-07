package pea.app.api.util

import java.io.File

import akka.util.ByteString
import pea.common.util.DateUtils
import play.api.http.{ContentTypes, HttpEntity}
import play.api.mvc.{ResponseHeader, Result}

object ResultUtils {

  def filesEntity(path: String, files: Array[File]): Result = {
    val html =
      s"""
         |<!DOCTYPE html>
         |<html lang="en">
         |<head>
         |    <meta charset="UTF-8">
         |    <meta name="viewport" content="width=device-width, initial-scale=1.0">
         |    <meta http-equiv="X-UA-Compatible" content="ie=edge">
         |    <title>${path}</title>
         |    <style>
         |        .item {
         |            height: 24px;
         |            line-height: 24px;
         |        }
         |        .item span {
         |            display: inline-block;
         |            vertical-align: middle;
         |            line-height: normal;
         |        }
         |        .type {
         |            color: red;
         |            width: 24px;
         |            height: 24px;
         |            display: inline-block;
         |        }
         |        .name {}
         |        .size {
         |            float: right;
         |            margin-right: 8px;
         |            color: lightseagreen;
         |        }
         |        .time {
         |            float: right;
         |            margin-right: 8px;
         |            font-size: small;
         |            color: lightslategray;
         |        }
         |    </style>
         |</head>
         |<body>${fileItems(path, files)}</body>
         |</html>
         |""".stripMargin
    Result(
      header = ResponseHeader(200),
      HttpEntity.Strict(ByteString(html), Some(ContentTypes.HTML))
    )
  }

  private def fileItems(path: String, files: Array[File]): String = {
    val sb = StringBuilder.newBuilder
    files.foreach(file => {
      sb.append(
        s"""
           |<div class="item">
           |   <span>${if (file.isDirectory) folderSVG else fileSVG}</span>
           |   <span class="name"><a href="/report/${path}/${file.getName}">${file.getName}</a></span>
           |   <span class="time">${DateUtils.parse(file.lastModified)}</span>
           |   <span class="size">${file.length}B</span>
           |</div>
           |""".stripMargin)
    })
    sb.toString()
  }

  val fileSVG = """<svg class="type" t="1572944229810" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="565" width="200" height="200"><path d="M853.333333 381.44a55.893333 55.893333 0 0 0-2.56-11.52v-3.84a45.653333 45.653333 0 0 0-8.106666-11.946667l-256-256a45.653333 45.653333 0 0 0-11.946667-8.106666h-3.84L557.226667 85.333333H298.666667a128 128 0 0 0-128 128v597.333334a128 128 0 0 0 128 128h426.666666a128 128 0 0 0 128-128V384v-2.56z m-256-150.613333L707.84 341.333333H597.333333zM768 810.666667a42.666667 42.666667 0 0 1-42.666667 42.666666H298.666667a42.666667 42.666667 0 0 1-42.666667-42.666666V213.333333a42.666667 42.666667 0 0 1 42.666667-42.666666h213.333333v213.333333a42.666667 42.666667 0 0 0 42.666667 42.666667h213.333333z" p-id="566" fill="#13227a"></path></svg>"""
  val folderSVG = """<svg class="type" t="1572939694992" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="1212" width="200" height="200"><path d="M136.436736 593.825792V461.78304c0-75.42272 61.090816-136.281088 136.649728-135.927808l469.368832-0.002048c75.469824 0.352256 139.720704 64.029696 142.649344 139.280384 0 0 2.291712 5.917696 2.397184 128.6912 0.104448 122.773504 0.045056 158.030848 0.045056 158.030848-0.0256 75.474944-61.190144 136.659968-136.493056 136.659968H272.884736c-75.358208 0-136.448-61.198336-136.448-136.612864V593.825792z" fill="#FFF200" p-id="1213"></path><path d="M258.899968 887.808c-68.788224-7.016448-122.463232-65.215488-122.463232-135.90528V461.78304c0-75.201536 60.744704-135.928832 135.996416-135.928832l0.653312 0.001024 469.368832 2.189312c75.469824 0.352256 139.720704 61.83936 142.649344 137.090048 0 0 2.291712 5.916672 2.397184 128.6912 0.104448 122.773504 0.045056 158.029824 0.045056 158.029824C887.523328 822.603776 833.78176 880.7936 765.0304 887.808H258.899968z m-24.66304-751.616h34.869248c43.022336 0 64.384 16.259072 91.429888 36.84352 29.256704 22.268928 65.668096 49.982464 127.637504 49.982464h301.5424c54.088704 0 98.092032 43.933696 98.092032 97.93536v39.656448a187.64288 187.64288 0 0 0-18.945024-21.154816c-16.712704-16.039936-35.934208-28.676096-57.132032-37.558272-22.088704-9.253888-45.33248-14.0032-69.087232-14.113792L273.273856 285.5936l-0.840704-0.002048c-23.80288 0-46.8992 4.653056-68.64896 13.831168-21.010432 8.864768-39.862272 21.556224-56.032256 37.71904A177.8688 177.8688 0 0 0 136.192 349.87008V234.10688c0-53.9904 43.982848-97.91488 98.044928-97.91488z m555.47904 45.865984H488.173568C389.128192 182.057984 379.7248 95.232 269.106176 95.232h-34.869248C157.58848 95.232 95.232 157.40928 95.232 234.10688v555.61216C95.232 866.41664 157.485056 928.768 234.277888 928.768h37.630976c0.324608 0 0.649216 0.011264 0.974848 0.011264h478.170112c0.325632 0 0.65024-0.01024 0.975872-0.012288h37.692416C866.458624 928.768 928.768 866.63168 928.768 789.98528V320.953344c0-76.619776-62.255104-138.89536-139.052032-138.89536z" fill="#000000" p-id="1214"></path></svg>"""
}
