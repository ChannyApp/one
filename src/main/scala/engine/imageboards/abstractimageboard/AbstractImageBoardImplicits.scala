package engine.imageboards.abstractimageboard

import engine.entities.BoardImplicits._
import engine.entities.PostImplicits._
import engine.entities.ThreadImplicits._
import engine.imageboards.abstractimageboard.AbstractImageBoardStructs.{Captcha, FetchPostsResponse, FormatPostRequest, FormatPostResponse}
import spray.json._


object AbstractImageBoardImplicits extends DefaultJsonProtocol with NullOptions {

  implicit object AbstractImageBoardFormat extends RootJsonFormat[AbstractImageBoard] {
    override def write(imageBoard: AbstractImageBoard): JsValue = {
      JsObject(
        "id" -> JsNumber(imageBoard.id),
        "name" -> JsString(imageBoard.name),
        "baseURL" -> JsString(imageBoard.baseURL),
        "captcha" -> imageBoard.captcha.toJson,
        "maxImages" -> JsNumber(imageBoard.maxImages),
        "logo" -> JsString(imageBoard.logo),
        "highlight" -> JsString(imageBoard.highlight),
        "boards" -> imageBoard.boards.toJson,
        "clipboardRegExps" -> imageBoard.clipboardRegExps.toJson
      )
    }

    override def read(json: JsValue): AbstractImageBoard = ???
  }

  implicit val formatPostResponseFormat: RootJsonFormat[FormatPostResponse] =
    jsonFormat2(FormatPostResponse)
  implicit val fetchPostsResponseFormat: RootJsonFormat[FetchPostsResponse] =
    jsonFormat2(FetchPostsResponse)
  implicit val captchaFormat: RootJsonFormat[Captcha] =
    jsonFormat2(Captcha)
  implicit val formatPostRequestFormat: RootJsonFormat[FormatPostRequest] =
    jsonFormat5(FormatPostRequest)
}