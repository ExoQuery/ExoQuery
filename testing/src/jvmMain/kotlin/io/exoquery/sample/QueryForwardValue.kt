package io.exoquery.sample

import io.exoquery.sql
import io.exoquery.PostgresDialect

data class Someone(val name: String)

fun main() {
  runBefore()
  runAfter()
}

fun runBefore() {
  val q = sql {
    MyExampleAhead.people().filter { p -> p.name == "Jack" }
  }
  val result = q.buildPretty<PostgresDialect>()
  println(result.value + " - " + result.debugData.phase)
}

object MyExampleAhead {
  // Need to see what happens with this after the previous and reversed
  fun people2() = sql {
    Table<Someone>().filter { it.name == "Jack" }
  }

  fun people() = sql {
    people2().filter { it.name == "Joe" }
  }
}

fun runAfter() {
  // This is just to show that the can run
  val q = sql {
    MyExampleAhead.people().filter { p -> p.name == "Jack" }
  }
  val result = q.buildPretty<PostgresDialect>()
  println(result.value + " - " + result.debugData.phase)
}
