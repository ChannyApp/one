package engine.imageboards.channy

import akka.http.scaladsl.model.headers.Cookie
import client.Client
import engine.entities.{Board, File, Post, Thread}
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs._
import engine.utils.RegExpRule

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class Channy(implicit client: Client) extends AbstractImageBoard {
  override val id: Int = 999
  override val name: String = "Channy"
  override val baseURL: String = "https://channy.io"
  override val captcha: Option[Captcha] = None
  override val maxImages: Int = 0
  override val logo: String = "https://avatars3.githubusercontent.com/u/45000974"
  override val highlight: String = "#4183C4"
  override val clipboardRegExps: List[String] = List("/мы/")

  override val boards: List[Board] = Await.result(this.fetchBoards(), Duration.Inf)

  override val regExps: List[RegExpRule] = List.empty

  println(s"[$name] Ready")

  override def fetchBoards(): Future[List[Board]] = {
    Future(
      List(
        Board(
          id = "x",
          name = "Channy Dev Team"
        )
      )
    )
  }

  override def fetchThreads(board: String)
                           (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, List[Thread]]] = {
    Future(
      Right(
        List(
          Thread(
            id = "0",
            URL = "https://channy.io",
            subject = "test",
            content = "News",
            postsCount = 1,
            timestampt = 1,
            files = List(
              File(
                name = "Kek",
                full = "https://ibin.co/w800/4jv6P7oHrMA9.jpg",
                thumbnail = "https://ibin.co/w800/4jv6P7oHrMA9.jpg"
              )
            ),
            decorations = List.empty,
            links = List.empty,
            replies = List.empty
          )
        )
      )
    )
  }

  override def fetchPosts(board: String, thread: Int, since: Int)
                         (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, FetchPostsResponse]] = {
    Future(
      Right(
        FetchPostsResponse(
          thread = Thread(
            id = "0",
            URL = "https://channy.io",
            subject = "test",
            content = "Привет, мы разработчики Channy",
            postsCount = 1,
            timestampt = 1,
            files = List(
              File(
                name = "Kek",
                full = "https://ibin.co/w800/4jv6P7oHrMA9.jpg",
                thumbnail = "https://ibin.co/w800/4jv6P7oHrMA9.jpg"
              )
            ),
            decorations = List.empty,
            links = List.empty,
            replies = List.empty
          ),
          posts = List(
            Post(
              id = "0",
              content = "Делаем для вас аппу епта",
              timestamp = 0,
              files = List(
                File(
                  name = "Kek",
                  full = "https://ibin.co/w800/4jv6P7oHrMA9.jpg",
                  thumbnail = "https://ibin.co/w800/4jv6P7oHrMA9.jpg"
                )
              ),
              decorations = List.empty,
              links = List.empty,
              replies = List.empty,
              selfReplies = List.empty
            )
          )
        )
      )
    )
  }

  override def formatPost(post: FormatPostRequest): FormatPostResponse = ???
}
