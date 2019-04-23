package engine.utils

import engine.entities.{DecorationMarkup, LinkMarkup, ReplyMarkup}

import scala.util.matching.Regex


object Extractor {
  def apply(text: String, regExpRules: List[RegExpRule]): Extracted = {
    val allMatches = this.rulesToMatches(text, regExpRules)

    if (allMatches.isEmpty)
      return Extracted(
        List.empty,
        List.empty,
        List.empty,
        text
      )

    // println(allMatches)

    val cleanContent = allMatches
      .foldLeft((text, 0))(
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

    val cleanMatches = allMatches
      .foldLeft(List.empty[RegExpMatch], 0)(
        (accumulator, current) => {
          val newMatch = RegExpMatch(
            start = current.start - accumulator._2,
            length = current.length,
            neededContent = current.neededContent,
            regexMatch = current.regexMatch,
            kind = current.kind
          )
          (newMatch :: accumulator._1, accumulator._2 + current.length)
        }
      )._1.reverse


    //    println(cleanMatches)

    val extracted = this.extractMarkups(cleanMatches.toVector)
    // println(extracted)

    Extracted(
      decorations = extracted.decorations,
      links = extracted.links,
      replies = extracted.replies,
      content = cleanContent
        .replaceAll("<br>", "\n")
        .replaceAll("<p>", "\n")
        .replaceAll("</p>", "\n")
        .trim()
    )
  }

  def rulesToMatches(text: String, regExpRules: List[RegExpRule]): List[RegExpMatch] = {
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
                  neededContent = m.group(1),
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
                  start = m.start,
                  length = m.end - m.start,
                  neededContent = m.group(1),
                  regexMatch = m,
                  kind = regExpRule.kind
                )
            )

          openMatches ::: closeMatches
        }
      )
      .sortWith((m1, m2) => {
        m1.regexMatch.start < m2.regexMatch.start
      })
  }

  def extractMarkups(matches: Vector[RegExpMatch], accumulator: Extracted = Extracted()): Extracted = {
    if (matches.isEmpty) {
      // println(accumulator.decorations)
      return Extracted(
        decorations = accumulator.decorations,
        links = accumulator.links,
        replies = accumulator.replies,
        content = accumulator.content
      )
    }


    // println(matches)
    val head = matches.head

    // TO FIX!
    val matchX = matches.indexWhere(m => m.neededContent == "/" + head.neededContent)
    if (matchX < 0) {
      println(s"BAD MARKUP? ${head.regexMatch.source}")
      println(matches.patch(0, Vector(), 1))

      return extractMarkups(
        matches.patch(0, Vector(), 1),
        accumulator
      )
    }

    val extracted = head.kind match {
      case _ => Extracted(
        decorations = accumulator.decorations ::: List(DecorationMarkup(
          start = head.start,
          end = matches(matchX).start,
          kind = head.kind
        )),
        links = accumulator.links,
        replies = accumulator.replies,
        content = accumulator.content
      )
    }

    extractMarkups(
      matches.tail.patch(matchX - 1, Vector(), 1),
      extracted
    )
  }
}

case class RegExpRule(
                       openRegex: Regex,
                       closeRegex: Regex,
                       kind: String
                     )

case class RegExpMatch(
                        start: Int,
                        length: Int,
                        neededContent: String,
                        regexMatch: Regex.Match,
                        kind: String
                      )

case class Extracted(
                      decorations: List[DecorationMarkup] = List.empty,
                      links: List[LinkMarkup] = List.empty,
                      replies: List[ReplyMarkup] = List.empty,
                      content: String = ""
                    )