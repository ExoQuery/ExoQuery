package io.exoquery.sample

import io.exoquery.SqlQuery
import io.exoquery.annotation.CapturedFunction
import io.exoquery.capture
import io.exoquery.sql.PostgresDialect

data class Someone(val name: String)

fun main() {
  runBefore()
  runAfter()
}

fun runBefore() {
  val q = capture {
    MyExampleAhead.people().filter { p -> p.name == "Jack" }
  }
  val result = q.buildPretty<PostgresDialect>()
  println(result.value + " - " + result.debugData.phase)
}

object MyExampleAhead {
  // Need to see what happens with this after the previous and reversed
  fun people2() = capture {
    Table<Someone>().filter { it.name == "Jack" }
  }

  fun people() = capture {
    people2().filter { it.name == "Joe" }
  }
}

fun runAfter() {
  // This is just to show that the can run
  val q = capture {
    MyExampleAhead.people().filter { p -> p.name == "Jack" }
  }
  val result = q.buildPretty<PostgresDialect>()
  println(result.value + " - " + result.debugData.phase)
}
