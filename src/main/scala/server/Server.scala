package server

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import engine.Engine
import engine.entities.ThreadImplicits._
import engine.imageboards.AbstractImageBoardImplicits._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}


class Server(engine: Engine)(implicit actorSystem: ActorSystem, materializer: ActorMaterializer, executionContext: ExecutionContext) {

  val route: Route =
    get {
      path("") {
        complete(engine.fetchImageBoards())
      } ~
        path(IntNumber / Segment) {
          case (imageBoardID, boardID) =>
            complete(engine.fetchImageBoardThreads(imageBoardID, boardID))
        } ~
        path(IntNumber / Segment / IntNumber) {
          case (imageBoardID, boardID, threadID) =>
            complete(engine.fetchImageBoardPosts(imageBoardID, boardID, threadID))
        } ~
        path(IntNumber / Segment / IntNumber / IntNumber) {
          case (imageBoardID, boardID, threadID, postNumber) =>
            complete(engine.fetchImageBoardPosts(imageBoardID, boardID, threadID, postNumber))
        }
    }

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
