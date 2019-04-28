package engine.utils

import engine.entities.{DecorationMarkup, LinkMarkup, ReplyMarkup}
import org.jsoup.parser.Parser

import scala.util.matching.Regex


object Extractor {
  def apply(text: String, regExpRules: List[RegExpRule]): Extracted = {
    val partiallyCleaned = Parser.unescapeEntities(
      text
        .replaceAll("<br>", "\n")
        .replaceAll("<p>", "\n")
        .replaceAll("</p>", "\n")
        .replaceAll("\n\n\n+", "\n"),
      false
    ).trim

    val allPairs: Seq[MatchPair] = this.rulesToMatchPairs(partiallyCleaned, regExpRules)

    if (allPairs.isEmpty)
      return Extracted(
        List.empty,
        List.empty,
        List.empty,
        partiallyCleaned
      )

    val paired = allPairs
      .flatMap(
        pair => {
          List(pair.open, pair.close)
        }
      )
      .sortWith((m1, m2) => m1.start < m2.start)

    //    println("ORIGINAL")
    //    paired.foreach(cp => println(cp))

    val cleanedContent = paired
      .foldLeft((partiallyCleaned, 0))(
        (accumulator, current) => {
          val content = accumulator._1
            .patch(
              current.start - accumulator._2,
              "",
              current.length
            )
          (content, accumulator._2 + current.length)
        }
      )._1

    val cleanedPairs = paired
      .foldLeft((List.empty[RegExpMatch], 0))(
        (accumulator, current) => {
          val updatedMatch = RegExpMatch(
            start = current.start - accumulator._2,
            length = current.length,
            regexMatch = current.regexMatch,
            kind = current.kind
          )
          (
            accumulator._1 ::: List(updatedMatch),
            accumulator._2 + current.length
          )
        }
      )._1
      .sortWith(
        (m1, m2) => {
          if (m1.kind == m2.kind)
            m1.start < m2.start
          else
            m1.kind < m2.kind
        })
      .grouped(2)
      .map(
        pair => MatchPair(
          open = pair.head,
          close = pair.last
        )
      )
      .toList
      .sortWith((m1, m2) => m1.open.start < m2.open.start)

    //    println("CLEANED")
    //    cleanedPairs.foreach(cp => println(cp))
    //
    //    println("==========")

    val extracted = try {
      this.extractMarkups(cleanedPairs)
    } catch {
      case _: Throwable => Extracted()
    }


    Extracted(
      decorations = extracted.decorations,
      links = extracted.links,
      replies = extracted.replies,
      content = cleanedContent
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

          openMatches
            .zip(closeMatches)
            .map(
              pair => MatchPair(
                open = pair._1,
                close = pair._2
              )
            )
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
                  kind = current.open.kind,
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