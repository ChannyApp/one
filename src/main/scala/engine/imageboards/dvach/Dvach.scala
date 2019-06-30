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
  override val label: String = "Абу благословил эту борду"
  override val highlight: String = "#F26722"

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
          .filter(element => !element.hasClass("post-reply-link"))
          .flatMap(
            element => {
              val elementText = element.wholeText()
              val indexes = Extractor.indexesOf(bodyText, elementText)
              indexes
                .map(
                  index => LinkMarkup(
                    start = index,
                    end = index + elementText.length,
                    kind = "external",
                    content = elementText,
                    link = element.attr("href")
                  )
                )
            }
          ).toList.distinct
      },
      body => {
        val elements = body.getElementsByClass("post-reply-link")
        val bodyText = body.wholeText()
        elements
          .iterator()
          .asScala
          .flatMap(
            element => {
              val elementText = element.wholeText()
              val indexes = Extractor.indexesOf(bodyText, elementText)
              indexes
                .map(
                  index => ReplyMarkup(
                    start = index,
                    end = index + elementText.length,
                    kind = "reply",
                    thread = BigInt(element.attr("data-thread")),
                    post = BigInt(element.attr("data-num"))
                  )
                )
            }
          ).toList.distinct
      }
    )
  }

  override def fetchBoards(): Future[List[Board]] = {
    this
      .client
      .getJSON(s"${this.baseURL}/boards.json")
      .map(
        _
          .asJsObject
          .getFields("boards")
          .head
          .convertTo[List[DvachBoardsResponse]]
          .map(
            board =>
              Board(id = board.id, name = board.name)
          ) ::: List(
          Board(id = "dev", name = "Тянач")
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
              .asJsObject
              .getFields("threads")
              .head
              .convertTo[List[DvachThreadsResponse]]
              .map(
                thread => {
                  val extracted = this.fetchMarkups(thread.comment)
                  Thread(
                    id = BigInt(thread.num),
                    URL = s"${this.baseURL}/$board/res/${thread.num}.html",
                    subject = Extractor.extractSimplifiedText(thread.subject),
                    content = extracted.content,
                    postsCount = thread.`posts_count` + 1,
                    timestamp = thread.timestamp,
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
      .getJSON(s"${this.baseURL}/makaba/mobile.fcgi?task=get_thread&board=$board&thread=$thread&post=${since + 1}")
      .map(_.convertTo[List[DvachPostsResponse]])
      .map(
        posts => {
          val formattedPosts = posts
            .map(
              post => {
                val extracted: Extracted = this.fetchMarkups(post.comment)
                Post(
                  id = BigInt(post.num),
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
          Right(
            FetchPostsResponse(
              thread = Thread(
                id = formattedPosts.head.id,
                URL = s"${this.baseURL}/$board/res/$thread.html",
                subject = posts
                  .head
                  .subject
                  .map(Extractor.extractSimplifiedText)
                  .getOrElse(formattedPosts.head.content),
                content = formattedPosts.head.content,
                postsCount = formattedPosts.length + 1,
                timestamp = formattedPosts.head.timestamp,
                files = formattedPosts.head.files,
                decorations = formattedPosts.head.decorations,
                links = formattedPosts.head.links,
                replies = formattedPosts.head.replies,
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