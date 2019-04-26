package engine.imageboards


import akka.http.scaladsl.HttpExt
import akka.stream.ActorMaterializer
import engine.entities.BoardImplicits._
import engine.entities.PostImplicits._
import engine.entities.ThreadImplicits._
import engine.entities.{Board, Post, Thread}
import engine.imageboards.AbstractImageBoardStructs.{Captcha, FetchPostsResponse, FormatPostRequest, FormatPostResponse}
import engine.utils.{Extracted, Extractor, RegExpRule}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractImageBoard(implicit executionContext: ExecutionContext, materializer: ActorMaterializer, client: HttpExt) {
  val id: Int
  val name: String
  val baseURL: String
  val captcha: Option[Captcha]
  val maxImages: Int
  val logo: String
  val highlight: String
  val clipboardRegExps: List[String]

  val boards: List[Board]
  val regExps: List[RegExpRule]

  def fetchBoards(): Future[List[Board]]

  def fetchThreads(board: String): Future[List[Thread]]

  def fetchPosts(board: String, thread: Int, since: Int): Future[FetchPostsResponse]

  def fetchMarkups(text: String): Extracted = Extractor(text, this.regExps)

  def formatPost(post: FormatPostRequest): FormatPostResponse
}

object AbstractImageBoardImplicits extends DefaultJsonProtocol with NullOptions {

  implicit object AbstractImageBoardFormat extends RootJsonFormat[AbstractImageBoard] {
    override def write(imageBoard: AbstractImageBoard): JsValue = {
      JsObject(
        "id" -> JsNumber(imageBoard.id),
        "name" -> JsString(imageBoard.name),
        "baseURL" -> JsString(imageBoard.baseURL),
        "captcha" -> imageBoard.captcha.toJson,
        "maxImages" -> JsNumber(imageBoard.maxImages),
        "logo" -> JsString(imageBoard.logo),
        "highlight" -> JsString(imageBoard.highlight),
        "boards" -> imageBoard.boards.toJson,
        "clipboardRegExps" -> imageBoard.clipboardRegExps.toJson
      )
    }

    override def read(json: JsValue): AbstractImageBoard = ???


  }

  implicit def formatPostResponseFormat: RootJsonFormat[FormatPostResponse] =
    jsonFormat2(FormatPostResponse)

  implicit val fetchPostsResponseFormat: RootJsonFormat[FetchPostsResponse] =
    jsonFormat2(FetchPostsResponse)
  implicit val captchaFormat: RootJsonFormat[Captcha] =
    jsonFormat2(Captcha)
  implicit val formatPostRequestFormat: RootJsonFormat[FormatPostRequest] =
    jsonFormat5(FormatPostRequest)

}

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

}
