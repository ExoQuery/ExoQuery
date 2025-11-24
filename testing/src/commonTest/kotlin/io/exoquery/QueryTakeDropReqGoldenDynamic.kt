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
    "take/postgres/XR" to kt(
      "Table(Person).take(4)"
    ),
    "take/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 4"
    ),
    "drop/sqlite/XR" to kt(
      "Table(Person).drop(4)"
    ),
    "drop/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT -1 OFFSET 4"
    ),
    "drop/postgres/XR" to kt(
      "Table(Person).drop(4)"
    ),
    "drop/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM Person x OFFSET 4"
    ),
    "take and drop/sqlite/XR" to kt(
      "Table(Person).take(3).drop(2)"
    ),
    "take and drop/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 3) AS x LIMIT -1 OFFSET 2"
    ),
    "take and drop/postgres/XR" to kt(
      "Table(Person).take(3).drop(2)"
    ),
    "take and drop/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 3) AS x OFFSET 2"
    ),
    "drop and limit/sqlite/XR" to kt(
      "Table(Person).drop(2).limit(3)"
    ),
    "drop and limit/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2"
    ),
    "drop and limit/postgres/XR" to kt(
      "Table(Person).drop(2).limit(3)"
    ),
    "drop and limit/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2"
    ),
    "offset and limit/sqlite/XR" to kt(
      "Table(Person).drop(2).limit(3)"
    ),
    "offset and limit/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2"
    ),
    "offset and limit/postgres/XR" to kt(
      "Table(Person).drop(2).limit(3)"
    ),
    "offset and limit/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2"
    ),
    "limit and drop/sqlite/XR" to kt(
      "Table(Person).limit(3).drop(2)"
    ),
    "limit and drop/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2"
    ),
    "limit and drop/postgres/XR" to kt(
      "Table(Person).limit(3).drop(2)"
    ),
    "limit and drop/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2"
    ),
    "limit and offset/sqlite/XR" to kt(
      "Table(Person).limit(3).drop(2)"
    ),
    "limit and offset/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2"
    ),
    "limit and offset/postgres/XR" to kt(
      "Table(Person).limit(3).drop(2)"
    ),
    "limit and offset/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2"
    ),
    "take and offset/sqlite/XR" to kt(
      "Table(Person).take(3).drop(2)"
    ),
    "take and offset/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 3) AS x LIMIT -1 OFFSET 2"
    ),
    "take and offset/postgres/XR" to kt(
      "Table(Person).take(3).drop(2)"
    ),
    "take and offset/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 3) AS x OFFSET 2"
    ),
    "drop, take, then drop/sqlite/XR" to kt(
      "Table(Person).drop(2).take(3).drop(1)"
    ),
    "drop, take, then drop/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2) AS x LIMIT -1 OFFSET 1"
    ),
    "drop, take, then drop/postgres/XR" to kt(
      "Table(Person).drop(2).take(3).drop(1)"
    ),
    "drop, take, then drop/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 3 OFFSET 2) AS x OFFSET 1"
    ),
    "take, drop, then take/sqlite/XR" to kt(
      "Table(Person).take(5).drop(2).take(2)"
    ),
    "take, drop, then take/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 5) AS x LIMIT 2 OFFSET 2"
    ),
    "take, drop, then take/postgres/XR" to kt(
      "Table(Person).take(5).drop(2).take(2)"
    ),
    "take, drop, then take/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 5) AS x LIMIT 2 OFFSET 2"
    ),
    "limit, offset, then limit/sqlite/XR" to kt(
      "Table(Person).limit(5).drop(2).limit(2)"
    ),
    "limit, offset, then limit/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 5 OFFSET 2) AS x LIMIT 2"
    ),
    "limit, offset, then limit/postgres/XR" to kt(
      "Table(Person).limit(5).drop(2).limit(2)"
    ),
    "limit, offset, then limit/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 5 OFFSET 2) AS x LIMIT 2"
    ),
    "take, take, drop/sqlite/XR" to kt(
      "Table(Person).take(5).take(3).drop(2)"
    ),
    "take, take, drop/sqlite/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 5) AS x LIMIT 3) AS x LIMIT -1 OFFSET 2"
    ),
    "take, take, drop/postgres/XR" to kt(
      "Table(Person).take(5).take(3).drop(2)"
    ),
    "take, take, drop/postgres/SQL" to cr(
      "SELECT x.name, x.age FROM (SELECT x.name, x.age FROM (SELECT x.name, x.age FROM Person x LIMIT 5) AS x LIMIT 3) AS x OFFSET 2"
    ),
  )
}
