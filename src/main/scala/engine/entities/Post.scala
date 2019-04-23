package engine.entities

import spray.json._
import DefaultJsonProtocol._
import FileImplicits._
import MarkupImplicits._

case class Post(
                 id: String,
                 content: String,
                 timestamp: Int,
                 files: List[File],
                 decorations: List[DecorationMarkup],
                 links: List[LinkMarkup],
                 replies: List[ReplyMarkup]
               )

object PostImplicits {
  implicit val postFormat: RootJsonFormat[Post] = jsonFormat7(Post)
}