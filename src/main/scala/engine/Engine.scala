package engine

import java.time.Duration

import akka.http.scaladsl.model.headers.Cookie
import client.Client
import com.github.benmanes.caffeine.cache.Caffeine
import engine.entities.Thread
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs.{ErrorResponse, FetchPostsResponse, FormatPostRequest, FormatPostResponse}
import engine.imageboards.dvach.Dvach
import engine.imageboards.fourchan.FourChan
import engine.imageboards.lolifox.Lolifox
import scalacache._
import scalacache.caffeine._
import scalacache.modes.try_._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try


class Engine(implicit client: Client) {
  val imageBoards: List[AbstractImageBoard] = List(
    new Dvach(),
    new FourChan(),
    new Lolifox(),
  )

  private implicit val threadsCache: Cache[List[Thread]] = CaffeineCache(
    Caffeine
      .newBuilder()
      .expireAfterWrite(Duration.ofSeconds(3))
      .build[String, Entry[List[Thread]]]
  )

  private implicit val postsCache: Cache[FetchPostsResponse] = CaffeineCache(
    Caffeine
      .newBuilder()
      .expireAfterWrite(Duration.ofSeconds(10))
      .build[String, Entry[FetchPostsResponse]]
  )

  def fetchImageBoards(): List[AbstractImageBoard] = {
    this.imageBoards
  }

  def fetchImageBoardThreads(id: Int, board: String)
                            (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, List[Thread]]] = {
    this.getImageBoardByID(id) match {
      case Right(imageboard) =>
        val key = id + board
        val posts: Try[Option[List[Thread]]] = get(key)
        posts.get match {
          case Some(cached) =>
            this.LOG(s"Cache HIT on ${imageboard.name} | $board")
            Future(Right(cached))
          case None =>
            this.LOG(s"Cache MISS on ${imageboard.name} | $board")
            imageboard.fetchThreads(board).map {
              case Right(value) =>
                put(key)(value)
                Right(value)
              case Left(value) => Left(value)
            }
        }
      case Left(error) => Future(Left(error))
    }
  }

  private def LOG(message: String): Unit = {
    println(s"[SERVER] $message")
  }

  def getImageBoardByID(id: Int): Either[ErrorResponse, AbstractImageBoard] = {
    this
      .imageBoards
      .find(IB => IB.id == id)
      .toRight(ErrorResponse("Imageboard doesn't exist"))
  }

  def fetchImageBoardPosts(id: Int, board: String, thread: Int, since: Int = 0)
                          (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, FetchPostsResponse]] = {
    this.getImageBoardByID(id) match {
      case Right(imageboard) =>
        val key = id + board + thread + since
        val posts: Try[Option[FetchPostsResponse]] = get(key)
        posts.get match {
          case Some(cached) =>
            this.LOG(s"Cache HIT on ${imageboard.name} | $board | $thread")
            Future(Right(cached))
          case None =>
            this.LOG(s"Cache MISS on ${imageboard.name} | $board | $thread")
            imageboard.fetchPosts(board, thread, since).map {
              case Right(value) =>
                put(key)(value)
                Right(value)
              case Left(value) => Left(value)
            }
        }
      case Left(error) => Future(Left(error))
    }
  }

  def formatImageBoardPost(id: Int, formatPostRequest: FormatPostRequest): Either[ErrorResponse, FormatPostResponse] = {
    this.getImageBoardByID(id).map(_.formatPost(formatPostRequest))
  }
}
