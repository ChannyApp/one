package engine.imageboards.lolifox

object LolifoxStructs {

  case class LolifoxBoardsResponse
  (
    uri: String,
    title: String
  )

  case class LolifoxFileResponse
  (
    filename: String,
    tim: String,
    ext: String
  )

  case class LolifoxThreadsResponse
  (
    no: Int,
    sub: Option[String],
    com: String,
    time: Int,
    replies: Int,
    filename: Option[String],
    tim: Option[String],
    ext: Option[String],
    `extra_files`: Option[List[LolifoxFileResponse]]
  )

  case class LolifoxPostsResponse
  (
    no: Int,
    com: Option[String],
    time: Int,
    filename: Option[String],
    tim: Option[String],
    ext: Option[String],
    `extra_files`: Option[List[LolifoxFileResponse]]
  )

  case class LolifoxFormatPostData
  (
    thread: String,
    board: String,
    body: String,
    `json_response`: Int = 1,
    post: String = "Ответить"
  )

}


