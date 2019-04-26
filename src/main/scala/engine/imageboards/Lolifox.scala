package engine.imageboards

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import engine.entities.{Board, Thread}
import engine.imageboards.AbstractImageBoardStructs.{Captcha, FetchPostsResponse, FormatPostRequest, FormatPostResponse}
import engine.imageboards.LolifoxImplicits._
import engine.imageboards.LolifoxStructs.LolifoxBoardsResponse
import engine.utils.RegExpRule
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class Lolifox(implicit executionContext: ExecutionContext, materializer: ActorMaterializer, client: HttpExt) extends AbstractImageBoard {
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
    //    RegExpRule(
    //      openRegex = raw"""<span class="quote">>""".r,
    //      closeRegex = raw"""<span class="quote">><(\/span)>""".r,
    //      "quote"
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
                .convertTo[List[LolifoxBoardsResponse]]
            )
      )
      .map(
        _
          .map(
            board =>
              Board(
                id = board.uri,
                name = board.title
              )
          )
      )

  }

  override def fetchThreads(board: String): Future[List[Thread]] = ???

  override def fetchPosts(board: String, thread: Int, since: Int): Future[FetchPostsResponse] = ???

  override def formatPost(post: FormatPostRequest): FormatPostResponse = ???
}

object LolifoxStructs {

  case class LolifoxBoardsResponse
  (
    uri: String,
    title: String
  )

}

object LolifoxImplicits {
  implicit val lolifoxBoardsResponseFormat: RootJsonFormat[LolifoxBoardsResponse] =
    jsonFormat2(LolifoxBoardsResponse)
}