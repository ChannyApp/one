package engine.imageboards.ylilauta

import akka.http.scaladsl.model.headers.HttpCookiePair
import akka.http.scaladsl.model.{DateTime, HttpMethods}
import client.Client
import engine.entities._
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import engine.utils.Extracted

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

class Ylilauta(implicit client: Client) extends AbstractImageBoard {
  override val id: Int = 4
  override val name: String = "Ylilauta"
  override val baseURL: String = "https://ylilauta.org"
  override val captcha: Option[Captcha] = Some(
    Captcha(
      url = "https://ylilauta.org/",
      kind = "reCAPTCHA v2",
      key = "XXX"
    )
  )
  override val maxImages: Int = 1
  override val logo: String = "https://channy.io/ylilauta-icon.png"
  override val label: String = "Юрок, доволен?"
  override val highlight: String = "#FFE"

  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  println(s"[$name] Ready")

  override def fetchMarkups(text: String): Extracted = ???

  override def fetchBoards(): Future[List[Board]] = {
    this
      .client
      .getHTML(this.baseURL, HttpMethods.POST)
      .map(
        body => {
          body
            .getElementById("boardselector")
            .getElementsByTag("a")
            .iterator()
            .asScala
            .map(
              board => Board(
                id = board.attr("href").drop(1).dropRight(1),
                name = board.ownText()
              )
            ).toList
        }
      )
  }

  override def fetchThreads(board: String)
                           (implicit cookies: List[HttpCookiePair]): Future[Either[ErrorResponse, List[Thread]]] = {
    this
      .client
      .getHTML(s"${this.baseURL}/$board/", HttpMethods.POST)
      .map(
        body => {
          Right(
            body
              .getElementsByClass("thread")
              .iterator()
              .asScala
              .map(
                element => {
                  val OP = element
                    .getElementsByClass("op_post")
                    .first()
                  val id = BigInt(OP.attr("data-id"))
                  val subject = OP
                    .getElementsByClass("postsubject")
                    .first()
                    .text()
                  val content = OP
                    .getElementsByClass("postcontent")
                    .first()
                    .wholeText()
                  val timestamp = DateTime.fromIsoDateTimeString(
                    OP
                      .getElementsByClass("posttime")
                      .first()
                      .attr("datetime")
                      .dropRight(5)
                  ).get
                  val postsCount = Try(
                    element
                      .getElementsByClass("replycount")
                      .first()
                      .wholeText()
                      .take(3)
                      .trim
                      .toInt
                  ).getOrElse(1)
                  val files = Option(
                    OP
                      .getElementsByClass("file-data")
                      .first()
                  ).map(
                    f => List(
                      File(
                        name = "File",
                        full = f.attr("src"),
                        thumbnail = f.attr("src")
                      )
                    )
                  ).getOrElse(List.empty)
                  Thread(
                    id = id,
                    URL = s"${this.baseURL}/$board/$id",
                    subject = subject,
                    content = content,
                    postsCount = postsCount,
                    timestamp = (timestamp.clicks / 1000).toInt,
                    files = files,
                    decorations = List.empty,
                    links = List.empty,
                    replies = List.empty
                  )
                }
              ).toList
          )
        }
      )
      .recover {
        case e: Exception =>
          e.printStackTrace()
          Left(ErrorResponse("Board unavailable"))
      }
  }

  override def fetchPosts(board: String, thread: Int, since: Int)
                         (implicit cookies: List[HttpCookiePair]): Future[Either[ErrorResponse, FetchPostsResponse]] = ???

  override def formatPost(post: FormatPostRequest): FormatPostResponse = ???
}
