package engine.imageboards.infinitechan

object InfiniteChanStructs {

  case class InfiniteChanBoardsResponse
  (
    uri: String,
    title: String
  )

  case class InfiniteChanFileResponse
  (
    filename: String,
    tim: String,
    ext: String
  )

  case class InfiniteChanThreadsResponse
  (
    no: Int,
    sub: Option[String],
    com: String,
    replies: Int,
    time: Int,
    filename: Option[String],
    tim: Option[String],
    ext: Option[String],
    `extra_files`: Option[List[InfiniteChanFileResponse]]
  )

  case class InfiniteChanPostsResponse
  (
    no: Int,
    com: Option[String],
    time: Int,
    filename: Option[String],
    tim: Option[String],
    ext: Option[String],
    `extra_files`: Option[List[InfiniteChanFileResponse]]
  )

  case class InfiniteChanFormatPostData
  (
    body: String,
    thread: String,
    board: String,
    `json_response`: Int = 1,
    post: String = "New Reply"
  )

}
