package engine.imageboards.dvach

import engine.imageboards.dvach.DvachStructs._
import spray.json.DefaultJsonProtocol._
import spray.json._

object DvachImplicits {
  implicit val dvachBoardsResponseFormat: RootJsonFormat[DvachBoardsResponse] =
    jsonFormat2(DvachBoardsResponse)
  implicit val dvachFileResponseFormat: RootJsonFormat[DvachFileResponse] =
    jsonFormat4(DvachFileResponse)
  implicit val dvachThreadsResponseFormat: RootJsonFormat[DvachThreadsResponse] =
    jsonFormat6(DvachThreadsResponse)
  implicit val dvachPostsResponseFormat: RootJsonFormat[DvachPostsResponse] =
    jsonFormat5(DvachPostsResponse)
  implicit val DvachFormatPostDataFormat: RootJsonFormat[DvachFormatPostData] =
    jsonFormat10(DvachFormatPostData)
}
