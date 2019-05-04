package engine.imageboards.abstractimageboard

import akka.http.scaladsl.model.headers.Cookie
import client.Client
import engine.entities.{Board, Post, Thread}
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import engine.utils.{Extracted, Extractor, RegExpRule}

import scala.concurrent.Future

abstract class AbstractImageBoard(implicit client: Client) {
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

  def fetchThreads(board: String)
                  (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, List[Thread]]]

  def fetchPosts(board: String, thread: Int, since: Int)
                (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, FetchPostsResponse]]

  def formatPost(post: FormatPostRequest): FormatPostResponse

  def fetchMarkups(text: String): Extracted = {
    Extractor(text, this.regExps)
  }

  def fetchSelfReplies(id: String, posts: List[Post]): List[String] = {
    posts
      .foldLeft(List.empty[String])(
        (accumulator, current) => {
          current.replies.exists(rp => rp.post == id) match {
            case true => accumulator ::: List(current.id)
            case false => accumulator
          }
        }
      )
  }
}




