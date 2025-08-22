package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object AggregateReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "value function aggregate/avg/XR" to kt(
      "select { Table(Person).map { it -> it.age }.avg_MC() }"
    ),
    "value function aggregate/avg/SQL" to cr(
      "SELECT (SELECT avg(it.age) FROM Person it) AS value"
    ),
    "value function aggregate/avg typed/XR" to kt(
      "select { Table(Person).map { it -> it.age }.avg_MC() }"
    ),
    "value function aggregate/avg typed/SQL" to cr(
      "SELECT (SELECT avg(it.age) FROM Person it) AS value"
    ),
    "value function aggregate/stdev/XR" to kt(
      "select { Table(Person).map { it -> it.age }.stddev_MC() }"
    ),
    "value function aggregate/stdev/SQL" to cr(
      "SELECT (SELECT stddev(it.age) FROM Person it) AS value"
    ),
    "value function aggregate/stdev typed/XR" to kt(
      "select { Table(Person).map { it -> it.age }.stddev_MC() }"
    ),
    "value function aggregate/stdev typed/SQL" to cr(
      "SELECT (SELECT stddev(it.age) FROM Person it) AS value"
    ),
    "column function aggregate/avg/XR" to kt(
      "select { Table(Person).map { it -> avg_GC(it.age) }.toExpr }"
    ),
    "column function aggregate/avg/SQL" to cr(
      "SELECT avg(it.age) AS value FROM Person it"
    ),
    "column function aggregate/avg typed/XR" to kt(
      "select { Table(Person).map { it -> avg_GC(it.age) }.toExpr }"
    ),
    "column function aggregate/avg typed/SQL" to cr(
      "SELECT avg(it.age) AS value FROM Person it"
    ),
    "column function aggregate/stdev/XR" to kt(
      "select { Table(Person).map { it -> stddev_GC(it.age) }.toExpr }"
    ),
    "column function aggregate/stdev/SQL" to cr(
      "SELECT stddev(it.age) AS value FROM Person it"
    ),
    "column function aggregate/stdev typed/XR" to kt(
      "select { Table(Person).map { it -> stddev_GC(it.age) }.toExpr }"
    ),
    "column function aggregate/stdev typed/SQL" to cr(
      "SELECT stddev(it.age) AS value FROM Person it"
    ),
  )
}
