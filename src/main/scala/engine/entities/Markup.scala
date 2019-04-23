package engine.entities

import spray.json._
import DefaultJsonProtocol._

case class DecorationMarkup(
                             start: Int,
                             end: Int,
                             kind: String
                           )

case class LinkMarkup(
                       start: Int,
                       end: Int,
                       kind: String,
                       content: String,
                       link: String
                     )

case class ReplyMarkup(
                        start: Int,
                        end: Int,
                        kind: String,
                        thread: String,
                        post: Int
                      )


object MarkupImplicits {
  implicit val decorationMarkupFormat: RootJsonFormat[DecorationMarkup] = jsonFormat3(DecorationMarkup)
  implicit val linkMarkupFormat: RootJsonFormat[LinkMarkup] = jsonFormat5(LinkMarkup)
  implicit val replyMarkupFormat: RootJsonFormat[ReplyMarkup] = jsonFormat5(ReplyMarkup)
}