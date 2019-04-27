package engine.imageboards.lolifox

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import engine.entities.{Board, File, Post, ReplyMarkup, Thread}
import engine.imageboards.abstractimageboard.AbstractImageBoard
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs.{Captcha, FetchPostsResponse, FormatPostRequest, FormatPostResponse}
import engine.imageboards.lolifox.LolifoxImplicits._
import engine.imageboards.lolifox.LolifoxStructs._
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
    RegExpRule(
      openRegex = raw"""(<span class="quote">)""".r,
      closeRegex = raw"""<span class="quote">.*(<\/span>)""".r,
      "quote"
    ),
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
                      .convertTo[List[LolifoxThreadsResponse]]
                  }
                )
            )
      )
      .map(
        _
          .map(
            thread => {
              val extracted = this.fetchMarkups(thread.com)
              Thread(
                id = thread.no.toString,
                subject = thread.sub
                  .map(
                    s => this
                      .fetchMarkups(s)
                      .content
                  )
                  .getOrElse(extracted.content),
                content = extracted.content,
                postsCount = thread.replies,
                timestampt = thread.time,
                files = List(
                  File(
                    name = thread.filename,
                    full = s"https://lolifox.org/$board/src/${thread.tim.concat(thread.ext)}",
                    thumbnail = "https://www.meme-arsenal.com/memes/0bcbd4fc3d35a9a4abe01ce8a3b21198.jpg"
                  )
                ) ::: thread.`extra_files`
                  .map(
                    files =>
                      files
                        .map(
                          file => File(
                            name = file.filename,
                            full = s"https://lolifox.org/$board/src/${file.tim.concat(file.ext)}",
                            thumbnail = "https://www.meme-arsenal.com/memes/0bcbd4fc3d35a9a4abe01ce8a3b21198.jpg"
                          )
                        )
                  ).getOrElse(List.empty),
                decorations = extracted.decorations,
                links = extracted.links,
                replies = extracted.replies
              )
            }
          )
      )
  }

  override def fetchPosts(board: String, thread: Int, since: Int): Future[FetchPostsResponse] = {
    val response: Future[HttpResponse] = client
      .singleRequest(
        HttpRequest(
          uri = s"${this.baseURL}/$board/res/$thread.json"
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
                .convertTo[List[LolifoxPostsResponse]]
            )
      )
      .map(
        posts => {
          val formattedPosts = posts
            .map(
              post => {
                val extracted = this.fetchMarkups(post.com.getOrElse(""))
                Post(
                  id = post.no.toString,
                  content = extracted.content,
                  timestamp = post.time,
                  files = post.filename.map(
                    filename => {
                      List(
                        File(
                          name = filename,
                          full = s"https://lolifox.org/$board/src/${post.tim.get.concat(post.ext.get)}",
                          thumbnail = "https://www.meme-arsenal.com/memes/0bcbd4fc3d35a9a4abe01ce8a3b21198.jpg"
                        )
                      ) ::: post.`extra_files`
                        .map(
                          files =>
                            files
                              .map(
                                file => File(
                                  name = file.filename,
                                  full = s"https://lolifox.org/$board/src/${file.tim.concat(file.ext)}",
                                  thumbnail = "https://www.meme-arsenal.com/memes/0bcbd4fc3d35a9a4abe01ce8a3b21198.jpg"
                                )
                              )
                        ).getOrElse(List.empty)
                    }
                  ).getOrElse(List.empty),
                  decorations = extracted.decorations,
                  links = extracted.links,
                  replies = extracted.replies,
                  selfReplies = List.empty
                )
              }
            )
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
              .drop(since)
          )
        }
      )
  }

  override def formatPost(post: FormatPostRequest): FormatPostResponse = {
    FormatPostResponse(
      url = s"${this.baseURL}/post.php",
      data = LolifoxFormatPostData(
        board = post.board,
        thread = post.thread,
        body = post.text
      ).toJson
    )
  }
}