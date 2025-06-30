package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object BlockStatementReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "blocks with map/XR" to kt(
      "Table(Person).map { it -> { val name = it.name; val age = it.age; Tuple(first = name, second = age) } }"
    ),
    "blocks with map/SQL" to cr(
      "SELECT it.name AS first, it.age AS second FROM Person it"
    ),
    "block with filter condition/XR" to kt(
      "Table(Person).filter { it -> { val age = it.age; val thirty = 30; age > thirty } }"
    ),
    "block with filter condition/SQL" to cr(
      "SELECT it.id, it.name, it.age FROM Person it WHERE it.age > 30"
    ),
    "block in query/XR" to kt(
      "{ val p = Table(Person).toExpr; p.filter { it -> it.age > 30 }.toExpr }.toQuery"
    ),
    "block in query/SQL" to cr(
      "SELECT it.id, it.name, it.age FROM Person it WHERE it.age > 30"
    ),
    "block with query clause/XR" to kt(
      "{ val p = Table(Person).toExpr; select { val p = from(p); val a = join(Table(Address)) { p.id == a.ownerId }; Tuple(first = p, second = a) }.toExpr }.toQuery"
    ),
    "block with query clause/SQL" to cr(
      "SELECT p.id, p.name, p.age, a.ownerId, a.street, a.city FROM Person p INNER JOIN Address a ON p.id = a.ownerId"
    ),
  )
}
