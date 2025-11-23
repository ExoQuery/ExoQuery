package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object QueryTakeDropReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "take/sqlite/XR" to kt(
      "Table(Person).take(4)"
    ),
    "take/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 4"
    ),
    "drop/sqlite/XR" to kt(
      "Table(Person).drop(4)"
    ),
    "drop/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT -1 OFFSET 4"
    ),
    "take and drop/sqlite/XR" to kt(
      "Table(Person).take(4).drop(4)"
    ),
    "take and drop/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 4) AS x LIMIT -1 OFFSET 4"
    ),
    "drop and limit/sqlite/XR" to kt(
      "Table(Person).drop(4).limit(4)"
    ),
    "drop and limit/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 4 OFFSET 4"
    ),
    "offset and limit/sqlite/XR" to kt(
      "Table(Person).drop(4).limit(4)"
    ),
    "offset and limit/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 4 OFFSET 4"
    ),
    "limit and drop/sqlite/XR" to kt(
      "Table(Person).limit(4).drop(4)"
    ),
    "limit and drop/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 4 OFFSET 4"
    ),
    "limit and offset/sqlite/XR" to kt(
      "Table(Person).limit(4).drop(4)"
    ),
    "limit and offset/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 4 OFFSET 4"
    ),
    "take and offset/sqlite/XR" to kt(
      "Table(Person).take(4).drop(4)"
    ),
    "take and offset/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 4) AS x LIMIT -1 OFFSET 4"
    ),
  )
}
