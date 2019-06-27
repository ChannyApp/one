package engine.imageboards.kohlchan

import akka.http.scaladsl.model.DateTime
import engine.imageboards.kohlchan.KohlChanStructs.{KohlChanBoardsResponse, KohlChanFileResponse, KohlChanPostsResponse, KohlChanThreadsResponse}
import spray.json.DefaultJsonProtocol._
import spray.json._

object KohlChanImplicits {

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    override def read(json: JsValue): DateTime =
      DateTime.fromIsoDateTimeString(json.toString.drop(1).dropRight(6)).get

    override def write(obj: DateTime): JsValue = ???
  }

  implicit val kohlChanBoardsResponseFormat: RootJsonFormat[KohlChanBoardsResponse] =
    jsonFormat2(KohlChanBoardsResponse)
  implicit val kohlChanThreadsResponseFormat: RootJsonFormat[KohlChanThreadsResponse] =
    jsonFormat6(KohlChanThreadsResponse)
  implicit val kohlChanFileResponseFormat: RootJsonFormat[KohlChanFileResponse] =
    jsonFormat3(KohlChanFileResponse)
  implicit val kohlChanPostsResponseFormat: RootJsonFormat[KohlChanPostsResponse] =
    jsonFormat5(KohlChanPostsResponse)
}
