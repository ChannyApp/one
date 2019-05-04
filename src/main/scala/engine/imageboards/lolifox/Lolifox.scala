package engine.imageboards.lolifox

import akka.http.scaladsl.model.headers.Cookie
import client.Client
import engine.entities.{Board, File, Post, ReplyMarkup, Thread}
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import engine.imageboards.lolifox.LolifoxImplicits._
import engine.imageboards.lolifox.LolifoxStructs._
import engine.utils.RegExpRule
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class Lolifox(implicit client: Client) extends AbstractImageBoard {
  override val id: Int = 2
  override val name: String = "Lolifox"
  override val baseURL: String = "https://lolifox.org"
  override val captcha: Option[Captcha] = None
  override val maxImages: Int = 4
  override val logo: String = "https://lolifox.org/static/Kuruminha_v2.png"
  override val highlight: String = "#EDDAD2"
  override val clipboardRegExps: List[String] = List("/просто лоли/")

  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  override val regExps: List[RegExpRule] = List(
    RegExpRule(
      openRegex = raw"""(<span class="quote">)""".r,
      closeRegex = raw"""<span class="quote">.*(<\/span>)""".r,
      "quote"
    ),
  )

  println(s"[$name] Ready")

  override def fetchBoards(): Future[List[Board]] = {
    this
      .client
      .GET(s"${this.baseURL}/boards.json")
      .map(
        _
          .convertTo[List[LolifoxBoardsResponse]]
          .map(
            board =>
              Board(
                id = board.uri,
                name = board.title
              )
          )
      )
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
                    .convertTo[List[LolifoxThreadsResponse]]
                }
              )
              .map(
                thread => {
                  val extracted = this.fetchMarkups(thread.com)
                  Thread(
                    id = thread.no.toString,
                    subject = thread.sub
                      .map(
                        s => this
                          .fetchMarkups(s)
                          .content
                      )
                      .getOrElse(extracted.content),
                    content = extracted.content,
                    postsCount = thread.replies + 1,
                    timestampt = thread.time,
                    files = thread.filename
                      .map(
                        filename =>
                          List(
                            File(
                              name = filename,
                              full = s"https://lolifox.org/$board/src/${thread.tim.get.concat(thread.ext.get)}",
                              thumbnail = s"https://lolifox.org/$board/src/${thread.tim.get.concat(thread.ext.get)}"
                            )
                          )
                      )
                      .getOrElse(List.empty) ::: thread.`extra_files`
                      .map(
                        files =>
                          files
                            .map(
                              file => File(
                                name = file.filename,
                                full = s"https://lolifox.org/$board/src/${file.tim.concat(file.ext)}",
                                thumbnail = s"https://lolifox.org/$board/src/${file.tim.concat(file.ext)}"
                              )
                            )
                      ).getOrElse(List.empty),
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
          Left(ErrorResponse("Доска недоступна"))
      }
  }

  override def fetchPosts(board: String, thread: Int, since: Int)
                         (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, FetchPostsResponse]] = {
    this
      .client
      .GET(s"${this.baseURL}/$board/res/$thread.json")
      .map(
        _
          .asJsObject
          .getFields("posts")
          .head
          .convertTo[List[LolifoxPostsResponse]]
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
                  files = post.filename.map(
                    filename => {
                      List(
                        File(
                          name = filename,
                          full = s"https://lolifox.org/$board/src/${post.tim.get.concat(post.ext.get)}",
                          thumbnail = s"https://lolifox.org/$board/src/${post.tim.get.concat(post.ext.get)}"
                        )
                      ) ::: post.`extra_files`
                        .map(
                          files =>
                            files
                              .map(
                                file => File(
                                  name = file.filename,
                                  full = s"https://lolifox.org/$board/src/${file.tim.concat(file.ext)}",
                                  thumbnail = s"https://lolifox.org/$board/src/${file.tim.concat(file.ext)}"
                                )
                              )
                        ).getOrElse(List.empty)
                    }
                  ).getOrElse(List.empty),
                  decorations = extracted.decorations,
                  links = extracted.links,
                  replies = extracted.replies,
                  selfReplies = List.empty
                )
              }
            )
          val originalPost: Post = formattedPosts.head
          Right(
            FetchPostsResponse(
              thread = Thread(
                id = originalPost.id,
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
          Left(ErrorResponse("Тред недоступен"))
      }
  }

  override def formatPost(post: FormatPostRequest): FormatPostResponse = {
    FormatPostResponse(
      url = s"${this.baseURL}/post.php",
      data = LolifoxFormatPostData(
        board = post.board,
        thread = post.thread,
        body = post.text
      ).toJson
    )
  }
}