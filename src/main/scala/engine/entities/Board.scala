package engine.entities

import spray.json._
import DefaultJsonProtocol._

case class Board(
                  id: String,
                  name: String
                )

object BoardImplicits {
  implicit val boardFormat: RootJsonFormat[Board] = jsonFormat2(Board)
}