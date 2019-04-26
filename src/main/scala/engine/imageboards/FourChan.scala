package engine.imageboards

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import engine.entities.{Board, File, Post, ReplyMarkup, Thread}
import engine.imageboards.AbstractImageBoardStructs.{Captcha, FetchPostsResponse, FormatPostRequest, FormatPostResponse}
import engine.imageboards.FourChanImplicits._
import engine.imageboards.FourChanStructs.{FourChanBoardsResponse, FourChanFormatPostData, FourChanPostsResponse, FourChanThreadsResponse}
import engine.utils.RegExpRule
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class FourChan(implicit executionContext: ExecutionContext, materializer: ActorMaterializer, client: HttpExt) extends AbstractImageBoard {
  override val id: Int = 1
  override val name: String = "4chan"
  override val baseURL: String = "https://a.4cdn.org"
  override val captcha: Captcha = Captcha(
    kind = "reCAPTCHA v2",
    key = "6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc"
  )
  override val maxImages: Int = 1
  override val logo: String = "https://s.4cdn.org/image/fp/logo-transparent.png"
  override val highlight: String = "#117743"
  override val clipboardRegExps: List[String] = List("/пиндоский форч/")

  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  override val regExps: List[RegExpRule] = List(
    //    RegExpRule(
    //      openRegex = raw"""<span class="quote">>""".r,
    //      closeRegex = raw"""<span class="quote">><(\/span)>""".r,
    //      "quote"
    //    ),
    //    RegExpRule(
    //      openRegex = raw"""<span class="deadlink">>""".r,
    //      closeRegex = raw"""<span class="deadlink">><(\/span)>""".r,
    //      "strikethrough"
    //    ),
  )

  println(s"[$name] Ready")

  override def fetchBoards(): Future[List[Board]] = {
    val response: Future[HttpResponse] = client
      .singleRequest(
        HttpRequest(
          uri = s"${this.baseURL}/boards.json"
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
                .getFields("boards")
                .head
                .convertTo[List[FourChanBoardsResponse]]
            )
      )
      .map(
        _
          .map(
            board =>
              Board(
                id = board.board,
                name = board.title
              )
          )
      )
  }

  override def fetchThreads(board: String): Future[List[Thread]] = {
    val response: Future[HttpResponse] = client
      .singleRequest(
        HttpRequest(
          uri = s"${this.baseURL}/$board/catalog.json"
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
            )
            .map(
              _
                .map(
                  thread => {
                    val extracted = this.fetchMarkups(thread.com.getOrElse(""))
                    Thread(
                      id = thread.no.toString,
                      subject = extracted.content,
                      content = extracted.content,
                      postsCount = thread.replies,
                      timestampt = thread.time,
                      files = thread.filename
                        .map(
                          filename => List(
                            File(
                              name = filename,
                              full = s"https://i.4cdn.org/$board/${thread.tim.get.toString.concat(thread.ext.get)}",
                              thumbnail = s"https://i.4cdn.org/$board/${thread.tim}.jpg"
                            )
                          )
                        ).getOrElse(List.empty),
                      extracted.decorations,
                      extracted.links,
                      extracted.replies,
                    )
                  }
                )
            )
      )

  }

  override def fetchPosts(board: String, thread: Int, since: Int): Future[FetchPostsResponse] = {
    val response: Future[HttpResponse] = client
      .singleRequest(
        HttpRequest(
          uri = s"${this.baseURL}/$board/thread/$thread.json"
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
                .getFields("posts")
                .head
                .convertTo[List[FourChanPostsResponse]]
            )
      )
      .map(
        _
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
                          full = s"https://i.4cdn.org/$board/${post.tim.get.toString.concat(post.ext.get)}",
                          thumbnail = s"https://i.4cdn.org/$board/${post.tim.get}.jpg"
                        )
                      )
                  ).getOrElse(List.empty),
                extracted.decorations,
                extracted.links,
                extracted.replies
              )
            }
          )
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
          )
        }
      )
  }


  override def formatPost(post: FormatPostRequest): FormatPostResponse = {
    FormatPostResponse(
      url = s"https://sys.4chan.org/${post.board}/post",
      data = FourChanFormatPostData(
        resto = post.thread,
        com = post.text,
        `g-recaptcha-response` = post.captcha
      ).toJson
    )
  }
}

object FourChanStructs {

  case class FourChanBoardsResponse
  (
    board: String,
    title: String
  )

  case class FourChanThreadsResponse
  (
    no: Int,
    com: Option[String],
    replies: Int,
    time: Int,
    filename: Option[String],
    tim: Option[Int],
    ext: Option[String]
  )

  case class FourChanPostsResponse
  (
    no: Int,
    com: Option[String],
    time: Int,
    filename: Option[String],
    tim: Option[Int],
    ext: Option[String]
  )

  case class FourChanFormatPostData
  (
    `MAX_FILE_SIZE`: Int = 2097152,
    mode: String = "regist",
    resto: String,
    com: String,
    images: List[String] = List("upfile"),
    `g-recaptcha-response`: Option[String]
  )

}

object FourChanImplicits {
  implicit val fourChanBoardsResponseFormat: RootJsonFormat[FourChanBoardsResponse] =
    jsonFormat2(FourChanBoardsResponse)
  implicit val fourChanThreadsResponseFormat: RootJsonFormat[FourChanThreadsResponse] =
    jsonFormat7(FourChanThreadsResponse)
  implicit val fourChanPostsResponseFormat: RootJsonFormat[FourChanPostsResponse] =
    jsonFormat6(FourChanPostsResponse)
  implicit val fourChanFormatPostDataFormat: RootJsonFormat[FourChanFormatPostData] =
    jsonFormat6(FourChanFormatPostData)
}