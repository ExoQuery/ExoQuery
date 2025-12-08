package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object SqlUdfReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "does necessary casts/XR" to kt(
      "Table(Person).map { p -> Tuple(first = p.age.toString_MC(), second = p.name.toInt_MC()) }"
    ),
    "does necessary casts/SQL" to cr(
      "SELECT CAST(p.age AS VARCHAR) AS first, CAST(p.name AS INTEGER) AS second FROM Person p"
    ),
    "does necessary casts - postgres/XR" to kt(
      "Table(Person).map { p -> Tuple(first = p.age.toString_MC(), second = p.name.toInt_MC()) }"
    ),
    "does necessary casts - postgres/SQL" to cr(
      "SELECT p.age::VARCHAR AS first, p.name::INTEGER AS second FROM Person p"
    ),
    "can handle de-nulling - element/XR" to kt(
      "Table(Test).map { p -> p.name }"
    ),
    "can handle de-nulling - element/SQL" to cr(
      "SELECT p.name AS value FROM Test p"
    ),
    "can handle de-nulling - row/XR" to kt(
      "select { val p = from(Table(Person)); val a = leftJoin(Table(Address)) { a.ownerId == p.id }; Tuple(first = p.name, second = a.city) }"
    ),
    "can handle de-nulling - row/SQL" to cr(
      "SELECT p.name AS first, a.city AS second FROM Person p LEFT JOIN Address a ON a.ownerId = p.id"
    ),
  )
}
