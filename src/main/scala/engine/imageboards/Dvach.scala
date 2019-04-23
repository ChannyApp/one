package engine.imageboards

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import engine.entities.{Board, File, Post, Thread}
import engine.imageboards.AbstractImageBoardStructs.{Captcha, FetchPostsResponse}
import engine.imageboards.DvachStructs.{DvachBoardsResponse, DvachPostsResponse, DvachThreadsResponse}
import engine.utils.{Extracted, RegExpRule}
import org.jsoup.Jsoup
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class Dvach(implicit executionContext: ExecutionContext, materializer: ActorMaterializer, client: HttpExt) extends AbstractImageBoard {
  override val id: Int = 0
  override val name: String = "Двач"
  override val baseURL: String = "https://2ch.hk"
  override val captcha: Captcha = Captcha(
    kind = "reCAPTCHA v2",
    key = "6LdwXD4UAAAAAHxyTiwSMuge1-pf1ZiEL4qva_xu"
  )
  override val maxImages: Int = 4
  override val logo: String = "https://2ch.hk/newtest/resources/images/dvlogo.png"
  override val highlight: String = "#F26722"
  override val clipboardRegExps: List[String] = List("/салямчик двачик/")

  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  //  override val regExps: List[RegExpRule] = List(
  //    RegExpRule(
  //      raw"""<strong>(.*?)</strong>""".r,
  //      "bold"
  //    ),
  //    RegExpRule(
  //      raw"""<b>(.*?)</b>""".r,
  //      "bold"
  //    ),
  //    RegExpRule(
  //      raw"""<em>(.*?)</em>""".r,
  //      "italics"
  //    ),
  //    RegExpRule(
  //      raw"""<span class="unkfunc">(.*?)<\/span>""".r,
  //      "quote"
  //    ),
  //    RegExpRule(
  //      raw"""<span class="spoiler">(.*?)<\/span>""".r,
  //      "spoiler"
  //    ),
  //    RegExpRule(
  //      raw"""<span class="s">(.*?)<\/span>""".r,
  //      "strikethrough"
  //    ),
  //    RegExpRule(
  //      raw"""<span class="u">(.*?)<\/span>""".r,
  //      "underline"
  //    ),
  //    RegExpRule(
  //      raw"""<a class="hashlink" href="(.*?)" .+?>(.*?)<\/a>""".r,
  //      "internal"
  //    ),
  //    RegExpRule(
  //      raw"""<a href="([^"]*)">(.*?)<\/a>""".r,
  //      "external"
  //    ),
  //    RegExpRule(
  //      raw"""<a.+?class="post-reply-link".+?data-num="(.*?)">.+?<\/a>""".r,
  //      "reply"
  //    ),
  //  )

  override val regExps: List[RegExpRule] = List(
    RegExpRule(
      raw"""<([\/]?strong)>""".r,
      "bold"
    ),
    RegExpRule(
      raw"""<([\/]?b)>""".r,
      "bold"
    ),
    RegExpRule(
      raw"""<([\/]?em)>""".r,
      "italics"
    ),
  )

  println(s"[$name] Ready")

  override def fetchBoards(): Future[List[Board]] = {
    val response: Future[HttpResponse] = client
      .singleRequest(
        HttpRequest(
          uri = s"$baseURL/makaba/mobile.fcgi?task=get_boards"
        )
      )

    response
      .flatMap(
        r =>
          Unmarshal(r)
            .to[String]
            .map(
              _
                .parseJson
                .convertTo[Map[String, List[DvachBoardsResponse]]]
            )
      )
      .map(
        _
          .values
          .flatten
          .map(
            x =>
              Board(id = x.id, name = x.name)
          )
          .toList
      )
  }

  override def fetchThreads(board: String): Future[List[Thread]] = {
    val response: Future[HttpResponse] = client
      .singleRequest(
        HttpRequest(
          uri = s"$baseURL/$board/catalog.json"
        )
      )

    response
      .flatMap(
        r =>
          Unmarshal(r)
            .to[String]
            .map(
              _
                .parseJson
                .asJsObject
                .getFields("threads")
                .head
                .convertTo[List[DvachThreadsResponse]]
            )
      )
      .map(
        _
          .map(
            thread => {
              val extracted = this.fetchMarkups(thread.comment)
              Thread(
                id = thread.num,
                subject = Jsoup.parse(thread.subject).text(),
                content = extracted.content,
                postsCount = thread.posts_count,
                timestampt = thread.timestamp,
                files = thread
                  .files
                  .map(
                    files =>
                      files
                        .map(
                          file =>
                            File(
                              name = file.fullname.getOrElse(file.displayname),
                              full = this.baseURL + file.path,
                              thumbnail = this.baseURL + file.thumbnail
                            )
                        )
                  )
                  .getOrElse(List[File]()),
                extracted.decorations,
                extracted.links,
                extracted.replies
              )
            }
          )
      )
  }

  override def fetchPosts(board: String, thread: Int, since: Int): Future[FetchPostsResponse] = {
    val response: Future[HttpResponse] = client
      .singleRequest(
        HttpRequest(
          uri = s"$baseURL/makaba/mobile.fcgi?task=get_thread&board=$board&thread=$thread&post=${since + 1}"
        )
      )

    response
      .flatMap(
        r =>
          Unmarshal(r)
            .to[String]
            .map(
              _
                .parseJson
                .convertTo[List[DvachPostsResponse]]
            )
      )
      .map(
        posts => {
          posts
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
                                name = file.fullname.getOrElse(file.displayname),
                                full = this.baseURL + file.path,
                                thumbnail = this.baseURL + file.thumbnail
                              )
                          )
                    )
                    .getOrElse(List[File]()),
                  extracted.decorations,
                  extracted.links,
                  extracted.replies
                )
              }
            )
        }
      )
      .map(
        formattedPosts => {
          val originalPost: Post = formattedPosts.head

          FetchPostsResponse(
            thread = Thread(
              id = originalPost.id,
              subject = originalPost.content,
              content = originalPost.content,
              postsCount = formattedPosts.length,
              timestampt = originalPost.timestamp,
              files = originalPost.files,
              decorations = originalPost.decorations,
              links = originalPost.links,
              replies = originalPost.replies,
            ),
            posts = formattedPosts
          )
        }
      )
  }
}

object DvachStructs {

  case class DvachBoardsResponse(
                                  id: String,
                                  name: String
                                )

  case class DvachFileResponse(
                                fullname: Option[String],
                                displayname: String,
                                path: String,
                                thumbnail: String
                              )

  case class DvachThreadsResponse(
                                   num: String,
                                   subject: String,
                                   comment: String,
                                   posts_count: Int,
                                   timestamp: Int,
                                   files: Option[List[DvachFileResponse]]
                                 )

  case class DvachPostsResponse(
                                 num: String,
                                 comment: String,
                                 timestamp: Int,
                                 files: Option[List[DvachFileResponse]]
                               )

  implicit val dvachBoardsResponseFormat: RootJsonFormat[DvachBoardsResponse] = jsonFormat2(DvachBoardsResponse)
  implicit val dvachFileResponseFormat: RootJsonFormat[DvachFileResponse] = jsonFormat4(DvachFileResponse)
  implicit val dvachThreadsResponseFormat: RootJsonFormat[DvachThreadsResponse] = jsonFormat6(DvachThreadsResponse)
  implicit val dvachPostsResponseFormat: RootJsonFormat[DvachPostsResponse] = jsonFormat4(DvachPostsResponse)
}