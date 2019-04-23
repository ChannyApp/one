package engine.entities

import spray.json._
import DefaultJsonProtocol._
import FileImplicits._
import MarkupImplicits._

case class Thread(
                   id: String,
                   subject: String,
                   content: String,
                   postsCount: Int,
                   timestampt: Int,
                   files: List[File],
                   decorations: List[DecorationMarkup],
                   links: List[LinkMarkup],
                   replies: List[ReplyMarkup]
                 )

object ThreadImplicits {
  implicit val threadFormat: RootJsonFormat[Thread] = jsonFormat9(Thread)
}

