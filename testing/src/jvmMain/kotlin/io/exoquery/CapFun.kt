package io.exoqueryCapturedFunctionParamKinds

import io.exoquery.*
import io.exoquery.annotation.SqlFragment
import io.exoquery.PostgresDialect

data class MyPerson(val id: Long, val name: String)

@SqlFragment
fun peopleNested() = sql { Table<MyPerson>().filter { p -> p.name == "foo" } }

@SqlFragment
fun people(filter: String) = sql { peopleNested() }

val q = sql.select {
  val p = from(people("Jack"))
  p
}

fun main() {
  val result = q.buildPrettyFor.Postgres()

  println(result.value)
}
