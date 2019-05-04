package engine.imageboards.abstractimageboard

import engine.entities.{Post, Thread}
import spray.json.JsValue

object AbstractImageBoardStructs {

  case class FetchPostsResponse
  (
    thread: Thread,
    posts: List[Post]
  )

  case class Captcha
  (
    kind: String,
    key: String
  )

  case class FormatPostRequest
  (
    board: String,
    thread: String,
    text: String,
    images: Int,
    captcha: Option[String]
  )

  case class FormatPostResponse
  (
    url: String,
    data: JsValue
  )

  case class ErrorResponse
  (
    error: String
  )

}