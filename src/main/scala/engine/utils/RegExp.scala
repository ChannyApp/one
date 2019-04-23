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

    println(allMatches)

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


    Extracted(
      decorations = List.empty,
      links = List.empty,
      replies = List.empty,
      content = cleanContent
    )
  }

  def rulesToMatches(text: String, regExpRules: List[RegExpRule]): List[RegExpMatch] = {
    regExpRules
      .flatMap(
        regExpRule =>
          regExpRule
            .regex
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
      )
      .sortWith((m1, m2) => {
        m1.regexMatch.start < m2.regexMatch.start
      })
  }
}

case class RegExpRule(
                       regex: Regex,
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
                      decorations: List[DecorationMarkup],
                      links: List[LinkMarkup],
                      replies: List[ReplyMarkup],
                      content: String
                    )