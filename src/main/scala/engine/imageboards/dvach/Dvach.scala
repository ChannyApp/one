package engine.imageboards.dvach

import akka.http.scaladsl.model.headers.HttpCookiePair
import client.Client
import engine.entities.{Board, File, LinkMarkup, Post, ReplyMarkup, Thread}
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import engine.imageboards.dvach.DvachImplicits._
import engine.imageboards.dvach.DvachStructs._
import engine.utils.{Extracted, Extractor}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class Dvach(implicit client: Client) extends AbstractImageBoard {
  override val id: Int = 0
  override val name: String = "Двач"
  override val baseURL: String = "https://2ch.hk"
  override val captcha: Option[Captcha] = Some(
    Captcha(
      url = "https://2ch.hk",
      kind = "reCAPTCHA v2",
      key = "6LdwXD4UAAAAAHxyTiwSMuge1-pf1ZiEL4qva_xu"
    )
  )
  override val maxImages: Int = 4
  override val logo: String = "https://channy.io/2ch-icon.png"
  override val highlight: String = "#F26722"
  override val clipboardRegExps: List[String] = List("/салямчик двачик/")
  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  val tags = List(
    ("b", "bold"),
    ("strong", "bold"),
    ("em", "italics")
  )
  val spans = List(
    ("s", "strikethrough"),
    ("unkfunc", "quote"),
    ("u", "underline"),
    ("spoiler", "spoiler")
  )

  println(s"[$name] Ready")

  override def fetchMarkups(text: String): Extracted = {
    Extractor(
      text,
      body => {
        val taggedElements = tags
          .flatMap(
            x =>
              body
                .getElementsByTag(x._1)
                .asScala
                .toList
                .map(element => (element, x._2))
          )
        val spannedElements = spans
          .flatMap(
            x =>
              body
                .getElementsByTag("span")
                .asScala
                .toList
                .flatMap(
                  element =>
                    element
                      .getElementsByClass(x._1)
                      .asScala
                      .toList
                      .map(element => (element, x._2))
                )
          )
        taggedElements ++ spannedElements
      },
      body => {
        val elements = body.getElementsByTag("a")
        val bodyText = body.wholeText()
        elements
          .iterator()
          .asScala
          .map(
            element => {
              val elementText = element.wholeText()
              val index = bodyText.indexOf(elementText)
              LinkMarkup(
                start = index,
                end = index + elementText.length,
                kind = "external",
                content = elementText,
                link = element.attr("href")
              )
            }
          ).toList
      },
      body => {
        val elements = body.getElementsByClass("post-reply-link")
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
                thread = element.attr("data-thread"),
                post = element.attr("data-num")
              )
            }
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
          .convertTo[List[DvachBoardsResponse]]
          .map(
            board =>
              Board(id = board.id, name = board.name)
          )
          .toList
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
      .GET(url = s"${this.baseURL}/$board/catalog.json")
      .map(
        response =>
          Right(
            response
              .asJsObject
              .getFields("threads")
              .head
              .convertTo[List[DvachThreadsResponse]]
              .map(
                thread => {
                  val extracted = this.fetchMarkups(thread.comment)
                  val subject = this.fetchMarkups(thread.subject).content

                  Thread(
                    id = thread.num,
                    URL = s"https://2ch.hk/$board/res/${thread.num}.html",
                    subject = subject,
                    content = extracted.content,
                    postsCount = thread.`posts_count` + 1,
                    timestampt = thread.timestamp,
                    files = thread
                      .files
                      .map(
                        files =>
                          files
                            .map(
                              file =>
                                File(
                                  name = file.fullname.getOrElse(file.displayname.getOrElse("File")),
                                  full = this.baseURL.concat(file.path),
                                  thumbnail = this.baseURL.concat(file.thumbnail)
                                )
                            )
                      )
                      .getOrElse(List.empty),
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
                         (implicit cookies: List[HttpCookiePair]): Future[Either[ErrorResponse, FetchPostsResponse]] = {
    this
      .client
      .GET(s"${this.baseURL}/makaba/mobile.fcgi?task=get_thread&board=$board&thread=$thread&post=${since + 1}")
      .map(_.convertTo[List[DvachPostsResponse]])
      .map(
        posts => {
          val formattedPosts = posts
            .map(
              post => {
                val extracted: Extracted = this.fetchMarkups(post.comment)
                Post(
                  id = post.num,
                  content = extracted.content,
                  timestamp = post.timestamp,
                  files = post
                    .files
                    .map(
                      files =>
                        files
                          .map(
                            file =>
                              File(
                                name = file.fullname.getOrElse(file.displayname.getOrElse("File")),
                                full = this.baseURL.concat(file.path),
                                thumbnail = this.baseURL.concat(file.thumbnail)
                              )
                          )
                    )
                    .getOrElse(List.empty),
                  decorations = extracted.decorations,
                  links = extracted.links,
                  replies = extracted.replies,
                  selfReplies = List.empty
                )
              }
            )
          val originalPost: Post = formattedPosts.head
          val subject = posts.head.subject
          Right(
            FetchPostsResponse(
              thread = Thread(
                id = originalPost.id,
                URL = s"https://2ch.hk/$board/res/$thread.html",
                subject = subject
                  .map(
                    s =>
                      this.fetchMarkups(s).content
                  )
                  .getOrElse(originalPost.content),
                content = originalPost.content,
                postsCount = formattedPosts.length + 1,
                timestampt = originalPost.timestamp,
                files = originalPost.files,
                decorations = originalPost.decorations,
                links = originalPost.links,
                replies = originalPost.replies,
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
      url = s"${this.baseURL}/makaba/posting.fcgi?json=1",
      headers = Map(
        "Referer" -> s"https://2ch.hk/${post.board}/"
      ),
      images = List.tabulate[String](post.images)(x => s"image${x + 1}"),
      data = DvachFormatPostData(
        board = post.board,
        thread = post.thread.orNull,
        subject = post.subject,
        comment = post.text,
        `captcha-key` = this.captcha.get.key,
        `g-recaptcha-response` = post.captcha
      ).toJson
    )
  }
}