package engine.imageboards.fourchan

object FourChanStructs {

  case class FourChanBoardsResponse
  (
    board: String,
    title: String
  )

  case class FourChanThreadsResponse
  (
    no: Int,
    com: Option[String],
    replies: Int,
    time: Int,
    filename: Option[String],
    tim: Option[BigInt],
    ext: Option[String]
  )

  case class FourChanPostsResponse
  (
    no: Int,
    resto: Int,
    com: Option[String],
    time: Int,
    filename: Option[String],
    tim: Option[BigInt],
    ext: Option[String]
  )

  case class FourChanFormatPostData
  (
    `MAX_FILE_SIZE`: Int = 2097152,
    mode: String = "regist",
    resto: String,
    com: String,
    images: List[String] = List("upfile"),
    `g-recaptcha-response`: Option[String]
  )

}
