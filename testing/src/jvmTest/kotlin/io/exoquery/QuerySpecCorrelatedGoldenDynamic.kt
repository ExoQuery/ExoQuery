package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object QuerySpecCorrelatedGoldenDynamic : GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "query with co-releated in filter - isNotEmpty/XR" to kt(
      "Table(Person).filter { p -> Table(Address).filter { a -> a.ownerId == p.id }.isNotEmpty_MC() }"
    ),
    "query with co-releated in filter - isNotEmpty" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE EXISTS (SELECT a.ownerId AS ownerId, a.street AS street, a.city AS city FROM Address a WHERE a.ownerId = p.id)"
    ),
    "query with co-releated in filter - isEmpty/XR" to kt(
      "Table(Person).filter { p -> Table(Address).filter { a -> a.ownerId == p.id }.isEmpty_MC() }"
    ),
    "query with co-releated in filter - isEmpty" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE NOT EXISTS (SELECT a.ownerId AS ownerId, a.street AS street, a.city AS city FROM Address a WHERE a.ownerId = p.id)"
    ),
    "query aggregation/min/XR" to kt(
      "Table(Person).filter { p -> Table(Address).map { a -> a.ownerId }.min_MC() == p.id }"
    ),
    "query aggregation/min" to cr(
      "SELECT p.id, p.name, p.age FROM Person p WHERE (SELECT min(a.ownerId) FROM Address a) = p.id"
    ),
    "select aggregation/min/XR" to kt(
      "Table(Address).map { a -> min_GC(a.ownerId) }"
    ),
    "select aggregation/min" to cr(
      "SELECT min(a.ownerId) AS value FROM Address a"
    ),
  )
}
