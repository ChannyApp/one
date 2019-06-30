package engine.imageboards.kohlchan

import akka.http.scaladsl.model.headers.HttpCookiePair
import client.Client
import engine.entities.{Board, File, Post, ReplyMarkup, Thread}
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import engine.imageboards.kohlchan.KohlChanImplicits._
import engine.imageboards.kohlchan.KohlChanStructs._
import engine.utils.{Extracted, Extractor}
import spray.json.DefaultJsonProtocol._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class KohlChan(implicit client: Client) extends AbstractImageBoard {
  override val id: Int = 3
  override val name: String = "Kohlchan"
  override val baseURL: String = "https://kohlchan.net"
  override val captcha: Option[Captcha] = Some(
    Captcha(
      url = "https://kohlchan.net/",
      kind = "reCAPTCHA v2",
      key = "XXX"
    )
  )
  override val maxImages: Int = 1
  override val logo: String = "https://channy.io/kohlchan-icon.png"
  override val label: String = ""
  override val highlight: String = "#EEEEEE"

  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  println(s"[$name] Ready")

  override def fetchMarkups(text: String): Extracted = {
    Extractor(
      text,
      body => {
        List.empty
      },
      body => {
        List.empty
      },
      body => {
        val elements = body.getElementsByClass("quoteLink")
        val bodyText = body.wholeText()
        elements
          .iterator()
          .asScala
          .map(
            element => {
              val elementText = element.wholeText()
              val index = bodyText.indexOf(elementText)
              ReplyMarkup(
                start = index,
                end = index + elementText.length,
                kind = "reply",
                thread = 0,
                post = BigInt(element.text().drop(2))
              )
            }
          ).toList
      }
    )
  }

  override def fetchBoards(): Future[List[Board]] = {
    this
      .client
      .getJSON(s"${this.baseURL}/boards.js?json=1")
      .map(
        _
          .asJsObject
          .getFields("data")
          .head
          .asJsObject
          .getFields("boards")
          .head
          .convertTo[List[KohlChanBoardsResponse]]
          .map(
            board =>
              Board(id = board.boardUri, name = board.boardName)
          )
      )
      .recover {
        case e: Exception =>
          e.printStackTrace()
          List.empty
      }
  }

  override def fetchThreads(board: String)
                           (implicit cookies: List[HttpCookiePair]): Future[Either[ErrorResponse, List[Thread]]] = {
    this
      .client
      .getJSON(url = s"${this.baseURL}/$board/catalog.json")
      .map(
        response =>
          Right(
            response
              .convertTo[List[KohlChanThreadsResponse]]
              .map(
                thread => {
                  val extracted = this.fetchMarkups(thread.markdown)
                  val subject = thread.subject.getOrElse(extracted.content)

                  Thread(
                    id = thread.threadId,
                    URL = s"${this.baseURL}/$board/res/${thread.threadId}.html",
                    subject = subject,
                    content = extracted.content,
                    postsCount = thread.postCount.getOrElse(1),
                    timestamp = (thread.lastBump.clicks / 1000).toInt,
                    files = List(
                      {
                        val parts = thread.thumb.splitAt(8)
                        val extension = parts._2 match {
                          case s if s.endsWith("jpeg") || s.endsWith("jpg") => ".jpg"
                          case s if s.endsWith("png") => ".png"
                          case s if s.endsWith("webm") => ".webm"
                          case s if s.endsWith("mp4") => ".mp4"
                          case s if s.endsWith("gif") => ".gif"
                        }
                        File(
                          name = "File",
                          full = s"${this.baseURL}${parts._1}${parts._2.drop(2)}$extension",
                          thumbnail = s"${this.baseURL}${thread.thumb}"
                        )
                      }
                    ),
                    decorations = extracted.decorations,
                    links = extracted.links,
                    replies = extracted.replies
                  )
                }
              )
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
      .getJSON(url = s"${this.baseURL}/$board/res/$thread.json")
      .map(
        response =>
          Right(
            {
              val OP = response.convertTo[KohlChanPostsResponse]
              val rest = response
                .asJsObject
                .getFields("posts")
                .head
                .convertTo[List[KohlChanPostsResponse]]
              val formattedPosts = (OP :: rest).map(
                post => {
                  val extracted = this.fetchMarkups(post.markdown)
                  Post(
                    id = post.postId.getOrElse(thread),
                    content = extracted.content,
                    timestamp = (post.creation.clicks / 1000).toInt,
                    files = post.files.map(
                      file =>
                        File(
                          name = file.originalName,
                          full = s"${this.baseURL}${file.path}",
                          thumbnail = s"${this.baseURL}${file.thumb}"
                        )
                    ),
                    decorations = extracted.decorations,
                    links = extracted.links,
                    replies = extracted.replies,
                    selfReplies = List.empty
                  )
                }
              )
              val extracted = this.fetchMarkups(OP.markdown)
              FetchPostsResponse(
                thread = Thread(
                  id = thread,
                  URL = s"${this.baseURL}/$board/res/$thread.html",
                  subject = OP.subject.getOrElse(extracted.content),
                  content = extracted.content,
                  postsCount = rest.length + 1,
                  timestamp = (OP.creation.clicks / 1000).toInt,
                  files = OP.files.map(
                    file =>
                      File(
                        name = file.originalName,
                        full = s"${this.baseURL}${file.path}",
                        thumbnail = s"${this.baseURL}${file.thumb}"
                      )
                  ),
                  decorations = extracted.decorations,
                  links = extracted.links,
                  replies = extracted.replies
                ),
                posts = formattedPosts
                  .map(
                    post =>
                      Post(
                        id = post.id,
                        content = post.content,
                        timestamp = post.timestamp,
                        files = post.files,
                        decorations = post.decorations,
                        links = post.links,
                        replies = post.replies,
                        selfReplies = this.fetchSelfReplies(post.id, formattedPosts)
                      )
                  )
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
