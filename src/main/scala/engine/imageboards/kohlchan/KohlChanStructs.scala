package engine.imageboards.kohlchan

import akka.http.scaladsl.model.DateTime

object KohlChanStructs {

  case class KohlChanBoardsResponse
  (
    boardUri: String,
    boardName: String
  )

  case class KohlChanThreadsResponse
  (
    threadId: Int,
    subject: Option[String],
    markdown: String,
    postCount: Option[Int],
    lastBump: DateTime,
    thumb: Option[String]
  )

  case class KohlChanFileResponse
  (
    originalName: String,
    path: String,
    thumb: String,
  )

  case class KohlChanPostsResponse
  (
    postId: Option[BigInt],
    subject: Option[String],
    markdown: String,
    creation: DateTime,
    files: List[KohlChanFileResponse]
  )

}
