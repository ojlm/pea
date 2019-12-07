package pea.app.model

object ResourceModels {

  case class ResourceCheckRequest(
                                   file: String, // relative path to `user-data` files
                                 )

  case class ResourceInfo(
                           exists: Boolean,
                           isDirectory: Boolean,
                           size: Long = 0L,
                           modified: Long = 0L,
                           md5: String = null,
                           filename: String = null,
                         )

  case class NewFolder(
                        path: String,
                        name: String,
                      )

}
