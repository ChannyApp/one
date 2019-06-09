package engine.imageboards.infinitechan

import engine.imageboards.infinitechan.InfiniteChanStructs._
import spray.json.DefaultJsonProtocol._
import spray.json._

object InfiniteChanImplicits {
  implicit val infiniteChanBoardsResponseFormat: RootJsonFormat[InfiniteChanBoardsResponse] =
    jsonFormat2(InfiniteChanBoardsResponse)
  implicit val infiniteChanFileResponseFormat: RootJsonFormat[InfiniteChanFileResponse] =
    jsonFormat3(InfiniteChanFileResponse)
  implicit val infiniteChanThreadsResponseFormat: RootJsonFormat[InfiniteChanThreadsResponse] =
    jsonFormat9(InfiniteChanThreadsResponse)
  implicit val infiniteChanPostsResponseFormat: RootJsonFormat[InfiniteChanPostsResponse] =
    jsonFormat7(InfiniteChanPostsResponse)
  implicit val infiniteChanFormatPostDataFormat: RootJsonFormat[InfiniteChanFormatPostData] =
    jsonFormat5(InfiniteChanFormatPostData)
}
