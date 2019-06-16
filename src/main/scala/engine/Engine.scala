package engine

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.headers.Cookie
import client.Client
import com.github.benmanes.caffeine.cache.Caffeine
import engine.entities.Thread
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs.{ErrorResponse, FetchPostsResponse, FormatPostRequest, FormatPostResponse}
import engine.imageboards.channy.Channy
import engine.imageboards.dvach.Dvach
import engine.imageboards.fourchan.FourChan
import engine.imageboards.infinitechan.InfiniteChan
import scalacache._
import scalacache.caffeine._
import scalacache.modes.try_._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class Engine(implicit client: Client) {
  private val imageBoards: List[AbstractImageBoard] = List(
    new Dvach(),
    new FourChan(),
    new InfiniteChan(),
    new Channy()
  )

  private implicit val threadsCache: Cache[Future[Either[ErrorResponse, List[Thread]]]] = CaffeineCache(
    Caffeine
      .newBuilder()
      .expireAfterWrite(3, TimeUnit.SECONDS)
      .build[String, Entry[Future[Either[ErrorResponse, List[Thread]]]]]
  )

  private implicit val postsCache: Cache[Future[Either[ErrorResponse, FetchPostsResponse]]] = CaffeineCache(
    Caffeine
      .newBuilder()
      .expireAfterWrite(10, TimeUnit.SECONDS)
      .build[String, Entry[Future[Either[ErrorResponse, FetchPostsResponse]]]]
  )

  private def LOG(message: String): Unit = {
    println(s"[SERVER] $message")
  }

  def fetchImageBoards(): List[AbstractImageBoard] = {
    this.imageBoards
  }

  def getImageBoardByID(id: Int): Either[ErrorResponse, AbstractImageBoard] = {
    this
      .imageBoards
      .find(IB => IB.id == id)
      .toRight(ErrorResponse("Imageboard doesn't exist"))
  }

  def fetchImageBoardThreads(id: Int, board: String)
                            (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, List[Thread]]] = {
    this.getImageBoardByID(id) match {
      case Right(imageboard) =>
        val key = id + board
        caching(key)(ttl = None) {
          this.LOG(s"Cache MISS on ${imageboard.name} | $board")
          imageboard.fetchThreads(board)
        }.get
      case Left(error) => Future(Left(error))
    }
  }

  def fetchImageBoardPosts(id: Int, board: String, thread: Int, since: Int = 0)
                          (implicit cookies: List[Cookie]): Future[Either[ErrorResponse, FetchPostsResponse]] = {
    this.getImageBoardByID(id) match {
      case Right(imageboard) =>
        val key = id + board + thread + since
        caching(key)(ttl = None) {
          this.LOG(s"Cache MISS on ${imageboard.name} | $board | $thread")
          imageboard.fetchPosts(board, thread, since)
        }.get
      case Left(error) => Future(Left(error))
    }
  }

  def formatImageBoardPost(id: Int, formatPostRequest: FormatPostRequest): Either[ErrorResponse, FormatPostResponse] = {
    this.getImageBoardByID(id).map(_.formatPost(formatPostRequest))
  }
}
