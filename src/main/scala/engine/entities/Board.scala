package engine.entities

import spray.json.DefaultJsonProtocol._
import spray.json._

case class Board
(
  id: String,
  name: String
)

object BoardImplicits {
  implicit val boardFormat: RootJsonFormat[Board] = jsonFormat2(Board)
}