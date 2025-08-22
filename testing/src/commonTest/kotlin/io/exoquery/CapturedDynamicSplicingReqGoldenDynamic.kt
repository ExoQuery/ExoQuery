package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object CapturedDynamicSplicingReqGoldenDynamic : GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "dynamic catpured function with join clauses/XR" to kt(
      """Table(Person).filter { p -> p.name == TagP("0") || p.name == TagP("1") || p.name == TagP("2") }"""
    ),
    "dynamic catpured function with join clauses/Params" to kt(
      "[ParamSingle(0, Joe, String), ParamSingle(1, Jack, String), ParamSingle(2, Jill, String)]"
    ),
    "dynamic catpured function with join clauses/SQL" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.name = {0:Joe} OR p.name = {1:Jack} OR p.name = {2:Jill}",
      "0" to "Joe", "1" to "Jack", "2" to "Jill"
    ),
  )
}
