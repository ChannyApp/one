package engine.utils

import engine.entities.{DecorationMarkup, LinkMarkup, ReplyMarkup}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object Extractor {
  def apply(
             text: String,
             getDecorations: Element => List[(Element, String)],
             getLinks: Element => List[LinkMarkup],
             getReplies: Element => List[ReplyMarkup]
           ): Extracted = {
    val body = Jsoup.parseBodyFragment(
      text
        .replaceAll("<br>", "\n")
        .replaceAll("\n\n\n+", "\n")
    ).body()

    val cleaned = body.wholeText()

    val decorations = processDecorations(cleaned, getDecorations(body))
    val links = getLinks(body)
    val replies = getReplies(body)

    Extracted(
      content = cleaned,
      decorations = decorations,
      links = links,
      replies = replies
    )
  }

  def processDecorations(bodyText: String, elements: List[(Element, String)]): List[DecorationMarkup] = {
    elements
      .flatMap(
        element => {
          val elementText = element._1.wholeText()
          indexesOf(bodyText, elementText)
            .map(
              start => {
                DecorationMarkup(
                  start = start,
                  end = start + elementText.length,
                  kind = element._2
                )
              }
            )
        }
      ).distinct
  }

  def indexesOf(source: String, target: String, index: Int = 0, withinOverlaps: Boolean = false): List[Int] = {
    if (source.isEmpty || target.isEmpty)
      return List.empty

    def recursive(indexTarget: Int = index, accumulator: List[Int] = Nil): List[Int] = {
      val position = source.indexOf(target, indexTarget)
      if (position == -1)
        accumulator
      else
        recursive(position + (if (withinOverlaps) 1 else target.length), position :: accumulator)
    }

    recursive().reverse
  }
}

case class Extracted
(
  decorations: List[DecorationMarkup] = List.empty,
  links: List[LinkMarkup] = List.empty,
  replies: List[ReplyMarkup] = List.empty,
  content: String = ""
)