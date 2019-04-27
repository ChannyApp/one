package engine.entities

import spray.json.DefaultJsonProtocol._
import spray.json._


case class File
(
  name: String,
  full: String,
  thumbnail: String,
  kind: String
)

object File {
  def apply(name: String, full: String, thumbnail: String): File = {
    new File(name, full, thumbnail, this.getKind(full))
  }

  def getKind(path: String): String = {
    val images: List[String] = List("png", "jpeg", "jpg", "gif")
    if (images.exists(ext => path.endsWith(ext)))
      return "image"

    val videos: List[String] = List("webm", "mp4")
    if (videos.exists(ext => path.endsWith(ext)))
      return "video"

    "unsupported"
  }
}

object FileImplicits {
  implicit val fileFormat: RootJsonFormat[File] = jsonFormat4(File.apply)
}