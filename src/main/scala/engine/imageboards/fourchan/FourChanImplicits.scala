package engine.imageboards.fourchan

import engine.imageboards.fourchan.FourChanStructs.{FourChanBoardsResponse, FourChanFormatPostData, FourChanPostsResponse, FourChanThreadsResponse}
import spray.json.DefaultJsonProtocol._
import spray.json._

object FourChanImplicits {
  implicit val fourChanBoardsResponseFormat: RootJsonFormat[FourChanBoardsResponse] =
    jsonFormat2(FourChanBoardsResponse)
  implicit val fourChanThreadsResponseFormat: RootJsonFormat[FourChanThreadsResponse] =
    jsonFormat7(FourChanThreadsResponse)
  implicit val fourChanPostsResponseFormat: RootJsonFormat[FourChanPostsResponse] =
    jsonFormat7(FourChanPostsResponse)
  implicit val fourChanFormatPostDataFormat: RootJsonFormat[FourChanFormatPostData] =
    jsonFormat5(FourChanFormatPostData)
}