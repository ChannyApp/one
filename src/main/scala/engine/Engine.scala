package engine

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.headers.HttpCookiePair
import client.Client
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import engine.entities.Thread
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs.{ErrorResponse, FetchPostsResponse, FormatPostRequest, FormatPostResponse}
import engine.imageboards.dvach.Dvach

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class Engine(implicit client: Client) {
  private val imageBoards: List[AbstractImageBoard] = List(
    new Dvach(),
    new FourChan()
  )

  private val threadsCache: Cache[String, List[Thread]] = Caffeine
    .newBuilder()
    .expireAfterWrite(3, TimeUnit.SECONDS)
    .build()

  private val postsCache: Cache[String, FetchPostsResponse] = Caffeine
    .newBuilder()
    .expireAfterWrite(5, TimeUnit.SECONDS)
    .build()

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
                            (implicit cookies: List[HttpCookiePair]): Future[Either[ErrorResponse, List[Thread]]] = {
    this.getImageBoardByID(id) match {
      case Right(imageboard) =>
        val key = id + board
        Option(threadsCache.getIfPresent(key)) match {
          case Some(threads) =>
            this.LOG(s"Cache HIT on ${imageboard.name} | $board")
            Future.successful(Right(threads))
          case None =>
            this.LOG(s"Cache MISS on ${imageboard.name} | $board")
            imageboard.fetchThreads(board).map {
              case Right(threads) =>
                threadsCache.put(key, threads)
                Right(threads)
              case Left(error) => Left(error)
            }
        }
      case Left(error) => Future.successful(Left(error))
    }
  }

  def fetchImageBoardPosts(id: Int, board: String, thread: Int, since: Int = 0)
                          (implicit cookies: List[HttpCookiePair]): Future[Either[ErrorResponse, FetchPostsResponse]] = {
    this.getImageBoardByID(id) match {
      case Right(imageboard) =>
        val key = id + board + thread + since
        Option(postsCache.getIfPresent(key)) match {
          case Some(posts) =>
            this.LOG(s"Cache HIT on ${imageboard.name} | $board | $thread")
            Future.successful(Right(posts))
          case None =>
            this.LOG(s"Cache MISS on ${imageboard.name} | $board | $thread")
            imageboard.fetchPosts(board, thread, since).map {
              case Right(posts) =>
                postsCache.put(key, posts)
                Right(posts)
              case Left(error) => Left(error)
            }
        }
      case Left(error) => Future.successful(Left(error))
    }
  }

  def formatImageBoardPost(id: Int, formatPostRequest: FormatPostRequest): Either[ErrorResponse, FormatPostResponse] = {
    this.getImageBoardByID(id).map(_.formatPost(formatPostRequest))
  }
}
