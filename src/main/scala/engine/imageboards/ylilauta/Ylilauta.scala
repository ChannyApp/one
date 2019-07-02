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
  override val label: String = ""
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
        body =>
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
                      .getElementsByClass("file-content")
                      .first()
                  ).map(
                    f => List(
                      File(
                        name = "File",
                        full = f.attr("href"),
                        thumbnail = f.attr("href")
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
      )
      .recover {
        case e: Exception =>
          e.printStackTrace()
          Left(ErrorResponse("Board unavailable"))
      }
  }

  override def fetchPosts(board: String, thread: Int, since: Int)
                         (implicit cookies: List[HttpCookiePair]): Future[Either[ErrorResponse, FetchPostsResponse]] = {
    this
      .client
      .getHTML(s"${this.baseURL}/$board/$thread", HttpMethods.POST)
      .map(
        body =>
          Right(
            {
              val OP = body
                .getElementsByClass("op_post")
                .first()

              val posts = body
                .getElementsByClass("post answer")
                .iterator()
                .asScala
                .map(
                  post => {
                    Post(
                      id = BigInt(post.attr("data-id")),
                      content = post
                        .getElementsByClass("postcontent")
                        .first()
                        .wholeText(),
                      timestamp = (DateTime.fromIsoDateTimeString(
                        post
                          .getElementsByClass("posttime")
                          .first()
                          .attr("datetime")
                          .dropRight(5)
                      ).get.clicks / 1000).toInt,
                      files = Option(
                        post
                          .getElementsByClass("file-content")
                          .first()
                      ).map(
                        f => List(
                          File(
                            name = "File",
                            full = f.attr("href"),
                            thumbnail = f.attr("href")
                          )
                        )
                      ).getOrElse(List.empty),
                      decorations = List.empty,
                      links = List.empty,
                      replies = List.empty,
                      selfReplies = Option(
                        post
                          .getElementsByClass("replies")
                          .first()
                      ).map(
                        element => {
                          element
                            .getElementsByTag("a")
                            .iterator()
                            .asScala
                            .map(
                              reply => {
                                BigInt(reply.attr("data-id"))
                              }
                            ).toList
                        }
                      ).getOrElse(List.empty)
                    )
                  }
                ).toList

              FetchPostsResponse(
                thread = Thread(
                  id = BigInt(OP.attr("data-id")),
                  URL = s"${this.baseURL}/$board/$id",
                  subject = OP
                    .getElementsByClass("postsubject")
                    .first()
                    .text(),
                  content = OP
                    .getElementsByClass("postcontent")
                    .first()
                    .wholeText(),
                  postsCount = 99999,
                  timestamp = (DateTime.fromIsoDateTimeString(
                    OP
                      .getElementsByClass("posttime")
                      .first()
                      .attr("datetime")
                      .dropRight(5)
                  ).get.clicks / 1000).toInt,
                  files = Option(
                    OP
                      .getElementsByClass("file-content")
                      .first()
                  ).map(
                    f => List(
                      File(
                        name = "File",
                        full = f.attr("href"),
                        thumbnail = f.attr("href")
                      )
                    )
                  ).getOrElse(List.empty),
                  decorations = List.empty,
                  links = List.empty,
                  replies = List.empty
                ),
                posts = posts
              )
            }
          )
      )
      .recover {
        case e: Exception =>
          e.printStackTrace()
          Left(ErrorResponse("Thread unavailable"))
      }
  }

  override def formatPost(post: FormatPostRequest): FormatPostResponse = ???
}
