package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object QueryValuePositionReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "counting query + from + toValue/XR" to kt(
      "select { val p = from(Table(Person)); Pair(first = p.name, second = select { val a = from(Table(Address)); where(a.street == 123 St.); count_GC(a.ownerId) }.toExpr) }"
    ),
    "counting query + from + toValue" to cr(
      "SELECT p.name AS first, (SELECT count(a.ownerId) FROM Address a WHERE a.street = '123 St.') AS second FROM Person p"
    ),
    "select a constant/XR" to kt(
      "select { 1 }"
    ),
    "select a constant" to cr(
      "SELECT 1 AS value"
    ),
  )
}
