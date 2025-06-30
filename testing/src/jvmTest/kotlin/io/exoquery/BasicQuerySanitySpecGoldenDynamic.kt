package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr

object BasicQuerySanitySpecGoldenDynamic : GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "basic query" to cr(
      "SELECT x.id, x.name, x.age FROM Person x"
    ),
    "query with map" to cr(
      "SELECT p.name AS value FROM Person p"
    ),
    "query with filter" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE p.age > 18"
    ),
    "query with flatMap" to cr(
      "SELECT a.ownerId, a.street, a.city FROM Person p, Address a WHERE a.ownerId = p.id"
    ),
  )
}
