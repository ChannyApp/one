package engine.imageboards.lolifox

import engine.imageboards.lolifox.LolifoxStructs._
import spray.json.DefaultJsonProtocol._
import spray.json._

object LolifoxImplicits {
  implicit val lolifoxBoardsResponseFormat: RootJsonFormat[LolifoxBoardsResponse] =
    jsonFormat2(LolifoxBoardsResponse)

  implicit val lolifoxFileResponseFormat: RootJsonFormat[LolifoxFileResponse] =
    jsonFormat3(LolifoxFileResponse)

  implicit val lolifoxThreadsResponseFormat: RootJsonFormat[LolifoxThreadsResponse] =
    jsonFormat9(LolifoxThreadsResponse)

  implicit val lolifoxPostsResponseFormat: RootJsonFormat[LolifoxPostsResponse] =
    jsonFormat7(LolifoxPostsResponse)

  implicit val lolifoxFormatPostDataFormat: RootJsonFormat[LolifoxFormatPostData] =
    jsonFormat5(LolifoxFormatPostData)
}