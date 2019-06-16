package engine.utils

import engine.entities.{DecorationMarkup, LinkMarkup, ReplyMarkup}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser


object Extractor {
  def apply(text: String, getReplies: Element => List[ReplyMarkup]): Extracted = {
    val partiallyCleaned = Parser.unescapeEntities(
      text
        .replaceAll("<br>", "\n")
        .replaceAll("\n\n\n+", "\n"),
      false
    )

    val body = Jsoup.parseBodyFragment(partiallyCleaned).body()

    Extracted(
      content = body.text().trim(),
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