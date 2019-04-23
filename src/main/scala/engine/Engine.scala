package engine

import java.time.Duration

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import com.github.benmanes.caffeine.cache.Caffeine
import engine.entities.{Post, Thread}
import engine.imageboards.AbstractImageBoardStructs.FetchPostsResponse
import engine.imageboards.{AbstractImageBoard, Dvach}
import scalacache._
import scalacache.caffeine._
import scalacache.modes.try_._

import scala.concurrent.{ExecutionContext, Future}


class Engine(implicit actorSystem: ActorSystem, materializer: ActorMaterializer, executionContext: ExecutionContext) {
  implicit val client: HttpExt = Http()

  val imageboards: List[AbstractImageBoard] = List(
    new Dvach()
  )

  implicit val threadsCache: Cache[Future[List[Thread]]] = CaffeineCache(
    Caffeine
      .newBuilder()
      .expireAfterWrite(Duration.ofSeconds(3))
      .build[String, Entry[Future[List[Thread]]]]
  )

  implicit val postsCache: Cache[Future[FetchPostsResponse]] = CaffeineCache(
    Caffeine
      .newBuilder()
      .expireAfterWrite(Duration.ofSeconds(10))
      .build[String, Entry[Future[FetchPostsResponse]]]
  )

  def getImageBoardByID(id: Int): Option[AbstractImageBoard] = {
    this.imageboards.find(IB => IB.id == id)
  }

  def fetchImageBoards(): List[AbstractImageBoard] = {
    this.imageboards
  }

  def fetchImageBoardThreads(id: Int, board: String): Future[List[Thread]] = {
    val key = id + board

    caching(key)(ttl = None) {
      this.getImageBoardByID(id)
        .map(imageboard => imageboard.fetchThreads(board))
        .getOrElse(Future(List[Thread]()))
    }.get
  }

  def fetchImageBoardPosts(id: Int, board: String, thread: Int, since: Int = 0): Future[FetchPostsResponse] = {
    val key = id + board + thread + since

    caching(key)(ttl = None) {
      this.getImageBoardByID(id)
        .map(imageboard => imageboard.fetchPosts(board, thread, since))
        .getOrElse(Future(FetchPostsResponse(
          thread = Thread(
            "", "", "", 0, 0, List.empty, List.empty, List.empty, List.empty
          ),
          posts = List.empty
        )))
    }.get
  }

  def formatImageBoardPost(id: Int): Post = {
    ???
  }
}
