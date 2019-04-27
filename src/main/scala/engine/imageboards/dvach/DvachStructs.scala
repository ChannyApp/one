package engine.imageboards.dvach

object DvachStructs {

  case class DvachBoardsResponse
  (
    id: String,
    name: String
  )

  case class DvachFileResponse
  (
    fullname: Option[String],
    displayname: String,
    path: String,
    thumbnail: String
  )

  case class DvachThreadsResponse
  (
    num: String,
    subject: String,
    comment: String,
    posts_count: Int,
    timestamp: Int,
    files: Option[List[DvachFileResponse]]
  )

  case class DvachPostsResponse
  (
    num: String,
    title: Option[String],
    comment: String,
    timestamp: Int,
    files: Option[List[DvachFileResponse]]
  )

  case class DvachFormatPostData
  (
    json: Int = 1,
    task: String = "post",
    board: String,
    thread: String,
    subject: String,
    comment: String,
    images: List[String],
    `captcha_type`: String = "recaptcha",
    `captcha-key`: String,
    `g-recaptcha-response`: Option[String]
  )

}