package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Cookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import engine.Engine
import engine.entities.ThreadImplicits._
import engine.imageboards.abstractimageboard.AbstractImageBoardImplicits._
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs.FormatPostRequest

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.util.{Failure, Success}


class Server(engine: Engine)
            (implicit actorSystem: ActorSystem, materializer: ActorMaterializer) {
  val route: Route =
    get {
      pathEndOrSingleSlash {
        complete(engine.fetchImageBoards())
      } ~
        ignoreTrailingSlash {
          extractRequest {
            request => {
              implicit val cookies: List[Cookie] = request
                .cookies
                .map(
                  cookie =>
                    Cookie(
                      name = cookie.name,
                      value = cookie.value
                    )
                ).toList

              path(IntNumber / Segment) {
                case (imageBoardID, boardID) =>
                  val response = engine.fetchImageBoardThreads(imageBoardID, boardID)
                  onComplete(response) {
                    case Success(value) =>
                      value match {
                        case Right(v) => complete(StatusCodes.OK -> v)
                        case Left(e) => complete(StatusCodes.BadRequest -> e)
                      }
                  }
              } ~
                path(IntNumber / Segment / IntNumber) {
                  case (imageBoardID, boardID, threadID) =>
                    val response = engine.fetchImageBoardPosts(imageBoardID, boardID, threadID)
                    onComplete(response) {
                      case Success(value) => value match {
                        case Right(v) => complete(StatusCodes.OK -> v)
                        case Left(e) => complete(StatusCodes.BadRequest -> e)
                      }
                    }
                } ~
                path(IntNumber / Segment / IntNumber / IntNumber) {
                  case (imageBoardID, boardID, threadID, postNumber) =>
                    val response = engine.fetchImageBoardPosts(imageBoardID, boardID, threadID, postNumber)
                    onComplete(response) {
                      case Success(value) => value match {
                        case Right(v) => complete(StatusCodes.OK -> v)
                        case Left(e) => complete(StatusCodes.BadRequest -> e)
                      }
                    }
                }
            }
          }
        }
    } ~
      post {
        path("format" / IntNumber) {
          imageBoardID => {
            entity(as[FormatPostRequest]) {
              postRequest =>
                complete(engine.formatImageBoardPost(imageBoardID, postRequest))
            }
          }
        }
      } ~
      get {
        ignoreTrailingSlash {
          path("proxy") {
            complete(StatusCodes.OK)
          }
        }
      }

  implicit val executionContext: ExecutionContextExecutor = this.actorSystem.dispatcher

  Http()
    .bindAndHandle(route, "0.0.0.0", 80)
    .onComplete {
      case Success(bound) =>
        println(s"[SERVER] Listening on ${bound.localAddress.getHostName} ${bound.localAddress.getPort}")
      case Failure(exception) =>
        exception.printStackTrace()
        actorSystem.terminate()
    }

  Await.result(actorSystem.whenTerminated, Duration.Inf)
}
