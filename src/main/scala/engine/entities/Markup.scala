package engine.entities

import spray.json.DefaultJsonProtocol._
import spray.json._

case class DecorationMarkup
(
  start: Int,
  end: Int,
  kind: String
)

case class LinkMarkup
(
  start: Int,
  end: Int,
  kind: String,
  content: String,
  link: String
)

case class ReplyMarkup
(
  start: Int,
  end: Int,
  kind: String,
  thread: String,
  post: String
)


object MarkupImplicits {
  implicit val decorationMarkupFormat: RootJsonFormat[DecorationMarkup] =
    jsonFormat3(DecorationMarkup)
  implicit val linkMarkupFormat: RootJsonFormat[LinkMarkup] =
    jsonFormat5(LinkMarkup)
  implicit val replyMarkupFormat: RootJsonFormat[ReplyMarkup] =
    jsonFormat5(ReplyMarkup)
}