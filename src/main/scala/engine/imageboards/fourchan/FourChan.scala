package engine.imageboards.fourchan

import akka.http.scaladsl.model.headers.Cookie
import client.Client
import engine.entities.{Board, File, Post, ReplyMarkup, Thread}
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import engine.imageboards.fourchan.FourChanImplicits._
import engine.imageboards.fourchan.FourChanStructs.{FourChanBoardsResponse, FourChanFormatPostData, FourChanPostsResponse, FourChanThreadsResponse}
import engine.utils.{Extracted, Extractor}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class FourChan(implicit client: Client) extends AbstractImageBoard {
  override val id: Int = 1
  override val name: String = "4chan"
  override val baseURL: String = "https://a.4cdn.org"
  override val captcha: Option[Captcha] = Some(
    Captcha(
      url = "https://boards.4chan.org",
      kind = "reCAPTCHA v2",
      key = "6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc"
    )
  )
  override val maxImages: Int = 1
  override val logo: String = "https://s.4cdn.org/image/fp/logo-transparent.png"
  override val highlight: String = "#117743"
  override val clipboardRegExps: List[String] = List("/пиндоский форч/")

  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  println(s"[$name] Ready")

  override def fetchMarkups(text: String): Extracted = {
    Extractor(
      text,
      element => {
        val elements = element.getElementsByClass("quotelink")
        elements.iterator().asScala.map(
          e => ReplyMarkup(
            start = 0,
            end = 0,
            kind = "reply",
            thread = e.attr("href"),
            post = e.attr("href").drop(2)
          )
        ).toList
      }
    )
  }

  override def fetchBoards(): Future[List[Board]] = {
    this
      .client
      .GET(s"${this.baseURL}/boards.json")
      .map(
        _
          .asJsObject
          .getFields("boards")
          .head
          .convertTo[List[FourChanBoardsResponse]]
          .map(
            board =>
              Board(
                id = board.board,
                name = board.title
              )
          )
      )
      .recover {
        case e: Exception =>
          e.printStackTrace()
          List.empty
      }
  }

  override def fetchThreads(board: String)
                           (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, List[Thread]]] = {
    this
      .client
      .GET(s"${this.baseURL}/$board/catalog.json")
      .map(
        response =>
          Right(
            response
              .convertTo[JsArray]
              .elements
              .toList
              .flatMap(
                page => {
                  page
                    .asJsObject
                    .getFields("threads")
                    .head
                    .convertTo[List[FourChanThreadsResponse]]
                }
              )
              .map(
                thread => {
                  val extracted = this.fetchMarkups(thread.com.getOrElse(""))
                  Thread(
                    id = thread.no.toString,
                    URL = s"https://boards.4chan.org/$board/thread/${thread.no}",
                    subject = extracted.content,
                    content = extracted.content,
                    postsCount = thread.replies + 1,
                    timestampt = thread.time,
                    files = thread.filename
                      .map(
                        filename =>
                          List(
                            File(
                              name = filename,
                              full = s"https://i.4cdn.org/$board/${thread.tim.get}${thread.ext.get}",
                              thumbnail = s"https://i.4cdn.org/$board/${thread.tim.get}s.jpg"
                            )
                          )
                      ).getOrElse(List.empty),
                    decorations = extracted.decorations,
                    links = extracted.links,
                    replies = extracted.replies,
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
                         (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, FetchPostsResponse]] = {
    this
      .client
      .GET(s"${this.baseURL}/$board/thread/$thread.json")
      .map(
        response =>
          response
            .asJsObject
            .getFields("posts")
            .head
            .convertTo[List[FourChanPostsResponse]]
      )
      .map(
        posts => {
          val formattedPosts = posts
            .map(
              post => {
                val extracted = this.fetchMarkups(post.com.getOrElse(""))
                Post(
                  id = post.no.toString,
                  content = extracted.content,
                  timestamp = post.time,
                  files = post.filename
                    .map(
                      filename =>
                        List(
                          File(
                            name = filename,
                            full = s"https://i.4cdn.org/$board/${post.tim.get}${post.ext.get}",
                            thumbnail = s"https://i.4cdn.org/$board/${post.tim.get}s.jpg"
                          )
                        )
                    ).getOrElse(List.empty),
                  decorations = extracted.decorations,
                  links = extracted.links,
                  replies = extracted.replies.map(
                    reply =>
                      ReplyMarkup(
                        start = reply.start,
                        end = reply.end,
                        kind = reply.kind,
                        thread = post.resto.toString,
                        post = reply.post
                      )
                  ),
                  selfReplies = List.empty
                )
              }
            )
          val originalPost: Post = formattedPosts.head
          Right(
            FetchPostsResponse(
              thread = Thread(
                id = originalPost.id,
                URL = s"https://boards.4chan.org/$board/thread/$id",
                subject = originalPost.content,
                content = originalPost.content,
                postsCount = formattedPosts.length + 1,
                timestampt = originalPost.timestamp,
                files = originalPost.files,
                decorations = originalPost.decorations,
                links = originalPost.links,
                replies = originalPost.replies.map(
                  reply =>
                    ReplyMarkup(
                      start = reply.start,
                      end = reply.end,
                      kind = reply.kind,
                      thread = originalPost.id,
                      post = reply.post
                    )
                ),
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
                .drop(since)
            )
          )
        }
      )
      .recover {
        case e: Exception =>
          e.printStackTrace()
          Left(ErrorResponse("Thread unavailable"))
      }
  }


  override def formatPost(post: FormatPostRequest): FormatPostResponse = {
    FormatPostResponse(
      url = s"https://sys.4chan.org/${post.board}/post",
      headers = Map(
        "Referer" -> "https://boards.4chan.org/"
      ),
      images = List("upfile"),
      data = FourChanFormatPostData(
        resto = post.thread.orNull,
        com = post.text,
        `g-recaptcha-response` = post.captcha
      ).toJson
    )
  }
}