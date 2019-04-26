package engine.utils

import engine.entities.{DecorationMarkup, LinkMarkup, ReplyMarkup}
import org.jsoup.parser.Parser

import scala.util.matching.Regex


object Extractor {
  def apply(text: String, regExpRules: List[RegExpRule]): Extracted = {
    val partiallyCleaned = text
      .replaceAll("<br>", "\n")
      .replaceAll("<p>", "\n")
      .replaceAll("</p>", "\n")
      .replaceAll("\n+", "\n")

    val allPairs = this.rulesToMatchPairs(partiallyCleaned, regExpRules)

    if (allPairs.isEmpty)
      return Extracted(
        List.empty,
        List.empty,
        List.empty,
        partiallyCleaned
      )

    val cleanedContent = allPairs
      .foldLeft((partiallyCleaned, 0))(
        (accumulator, current) => {
          val content = accumulator._1
            .patch(
              current.open.start - accumulator._2,
              "",
              current.open.length
            )
            .patch(
              current.close.start - current.open.length - accumulator._2,
              "",
              current.close.length
            )

          (content, accumulator._2 + current.open.length + current.close.length)
        }
      )._1

    val cleanedPairs = allPairs
      .foldLeft((List.empty[MatchPair], 0))(
        (accumulator, current) => {
          val updatedMatch = MatchPair(
            open = RegExpMatch(
              start = current.open.start - accumulator._2,
              length = current.open.length,
              regexMatch = current.open.regexMatch,
              kind = current.open.kind
            ),
            close = RegExpMatch(
              start = current.close.start - current.open.length - accumulator._2,
              length = current.close.length,
              regexMatch = current.close.regexMatch,
              kind = current.close.kind
            )
          )
          (
            accumulator._1 ::: List(updatedMatch),
            accumulator._2 + current.open.length + current.close.length
          )
        }
      )._1

    val extracted = this.extractMarkups(cleanedPairs)

    Extracted(
      decorations = extracted.decorations,
      links = extracted.links,
      replies = extracted.replies,
      content = Parser.unescapeEntities(cleanedContent, false)
    )
  }

  def rulesToMatchPairs(text: String, regExpRules: List[RegExpRule]): List[MatchPair] = {
    regExpRules
      .flatMap(
        regExpRule => {
          val openMatches = regExpRule
            .openRegex
            .findAllMatchIn(text)
            .toList
            .map(
              m =>
                RegExpMatch(
                  start = m.start,
                  length = m.end - m.start,
                  regexMatch = m,
                  kind = regExpRule.kind
                )
            )

          val closeMatches = regExpRule
            .closeRegex
            .findAllMatchIn(text)
            .toList
            .map(
              m =>
                RegExpMatch(
                  start = m.end - m.group(1).length,
                  length = m.end - (m.end - m.group(1).length),
                  regexMatch = m,
                  kind = regExpRule.kind
                )
            )

          val allPairs: List[MatchPair] = openMatches
            .zip(closeMatches)
            .map(
              pair => MatchPair(
                open = pair._1,
                close = pair._2
              )
            )

          allPairs
        }
      )
      .sortWith((m1, m2) => m1.open.start < m2.open.start)
  }

  def extractMarkups(pairs: List[MatchPair]): Extracted = {
    pairs.foldLeft(Extracted())(
      (accumulator, current) => {

        current.open.kind match {
          case "reply" =>
            Extracted(
              decorations = accumulator.decorations,
              links = accumulator.links,
              replies = accumulator.replies ::: List(
                ReplyMarkup(
                  start = current.open.start,
                  end = current.close.start,
                  kind = "reply",
                  thread = current.open.regexMatch.group(2),
                  post = current.open.regexMatch.group(3)
                )
              )
            )
          case _ =>
            Extracted(
              decorations = accumulator.decorations ::: List(
                DecorationMarkup(
                  start = current.open.start,
                  end = current.close.start,
                  kind = current.open.kind
                )
              ),
              links = accumulator.links,
              replies = accumulator.replies,
            )
        }
      }
    )
  }
}

case class RegExpRule
(
  openRegex: Regex,
  closeRegex: Regex,
  kind: String
)

case class RegExpMatch
(
  start: Int,
  length: Int,
  regexMatch: Regex.Match,
  kind: String
)

case class MatchPair
(
  open: RegExpMatch,
  close: RegExpMatch
)

case class Extracted
(
  decorations: List[DecorationMarkup] = List.empty,
  links: List[LinkMarkup] = List.empty,
  replies: List[ReplyMarkup] = List.empty,
  content: String = ""
)