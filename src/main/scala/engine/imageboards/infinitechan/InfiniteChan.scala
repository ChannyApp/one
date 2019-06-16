package engine.imageboards.infinitechan

import akka.http.scaladsl.model.headers.Cookie
import client.Client
import engine.entities.{Board, File, Post, ReplyMarkup, Thread}
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import engine.imageboards.infinitechan.InfiniteChanImplicits._
import engine.imageboards.infinitechan.InfiniteChanStructs._
import engine.utils.{Extracted, Extractor}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class InfiniteChan(implicit client: Client) extends AbstractImageBoard {
  override val id: Int = 2
  override val name: String = "8chan"
  override val baseURL: String = "https://8ch.net"
  override val captcha: Option[Captcha] = Some(
    Captcha(
      url = "https://8ch.net/dnsbls_bypass.php",
      kind = "Custom",
      key = ""
    )
  )
  override val maxImages: Int = 5
  override val logo: String = "https://1d4chan.org/images/b/bc/8chan_logo.png"
  override val highlight: String = "#EEF2FF"
  override val clipboardRegExps: List[String] = List("/пиндоский инфинит чат/")

  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  println(s"[$name] Ready")

  override def fetchMarkups(text: String): Extracted = {
    Extractor(
      text,
      element => {
        val elements = element.getElementsByAttributeValueStarting("onclick", "highlightReply")
        elements.iterator().asScala.map(
          e => ReplyMarkup(
            start = 0,
            end = 0,
            kind = "reply",
            thread = e.attr("href").takeRight(20).dropRight(13),
            post = e.attr("href").takeRight(7)
          )
        ).toList
      }
    )
  }

  override def fetchBoards(): Future[List[Board]] = {
    this
      .client
      .GET(s"${this.baseURL}/boards-top20.json")
      .map(
        _
          .convertTo[List[InfiniteChanBoardsResponse]]
          .map(
            board =>
              Board(
                id = board.uri,
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
                    .convertTo[List[InfiniteChanThreadsResponse]]
                }
                  .map(
                    thread => {
                      val extracted = this.fetchMarkups(thread.com)
                      Thread(
                        id = thread.no.toString,
                        URL = s"https://8ch.net/$board/res/${thread.no}.html",
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
                                  full = s"https://media.8ch.net/file_store/${thread.tim.get.concat(thread.ext.get)}",
                                  thumbnail = s"https://media.8ch.net/file_store/thumb/${thread.tim.get.concat(thread.ext.get)}"
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
                                    full = s"https://media.8ch.net/file_store/${file.tim.concat(file.ext)}",
                                    thumbnail = s"https://media.8ch.net/file_store/thumb/${file.tim.concat(file.ext)}"
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
      .GET(s"${this.baseURL}/$board/res/$thread.json")
      .map(
        _
          .asJsObject
          .getFields("posts")
          .head
          .convertTo[List[InfiniteChanPostsResponse]]
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
                          full = s"https://media.8ch.net/file_store/${post.tim.get.concat(post.ext.get)}",
                          thumbnail = s"https://media.8ch.net/file_store/thumb/${post.tim.get.concat(post.ext.get)}"
                        )
                      ) ::: post.`extra_files`
                        .map(
                          files =>
                            files
                              .map(
                                file => File(
                                  name = file.filename,
                                  full = s"https://media.8ch.net/file_store/${file.tim.concat(file.ext)}",
                                  thumbnail = s"https://media.8ch.net/file_store/thumb/${file.tim.concat(file.ext)}"
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
                URL = s"https://8ch.net/$board/res/$id.html",
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
      url = "https://sys.8ch.net/post.php",
      headers = Map(
        "Referer" -> "https://8ch.net/"
      ),
      images = List("file") ++ List.tabulate[String](post.images - 1)(x => s"file${x + 2}"),
      data = InfiniteChanFormatPostData(
        body = post.text,
        board = post.board,
        thread = post.thread.orNull,
      ).toJson
    )
  }
}
