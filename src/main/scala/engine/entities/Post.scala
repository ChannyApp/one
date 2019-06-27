package engine.entities

import engine.entities.FileImplicits._
import engine.entities.MarkupImplicits._
import spray.json.DefaultJsonProtocol._
import spray.json._

case class Post
(
  id: BigInt,
  content: String,
  timestamp: Int,
  files: List[File],
  decorations: List[DecorationMarkup],
  links: List[LinkMarkup],
  replies: List[ReplyMarkup],
  selfReplies: List[BigInt]
)

object PostImplicits {
  implicit val postFormat: RootJsonFormat[Post] = jsonFormat8(Post)
}