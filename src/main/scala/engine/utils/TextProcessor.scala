package engine.utils

import engine.entities.{DecorationMarkup, LinkMarkup, ReplyMarkup}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element


object Extractor {
  def apply(text: String, getReplies: Element => List[ReplyMarkup]): Extracted = {
    val body = Jsoup.parseBodyFragment(
      text
        .replaceAll("<br>", "\n")
        .replaceAll("\n\n\n+", "\n")
    ).body()

    val cleaned = body
      .wholeText()
      .trim()

    Extracted(
      content = cleaned,
      replies = getReplies(body)
    )
  }
}

case class Extracted
(
  decorations: List[DecorationMarkup] = List.empty,
  links: List[LinkMarkup] = List.empty,
  replies: List[ReplyMarkup] = List.empty,
  content: String = ""
)