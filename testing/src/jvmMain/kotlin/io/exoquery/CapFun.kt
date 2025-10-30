package io.exoqueryCapturedFunctionParamKinds

import io.exoquery.*
import io.exoquery.annotation.SqlFragment
import io.exoquery.PostgresDialect

data class MyPerson(val id: Long, val name: String)

val q = sql.select {
  val p = from(MyCaptureAheadObject.people("Jack"))
  p
}


object MyCaptureAheadObject {
  @SqlFragment
  fun peopleNested(filter: String) = sql { Table<MyPerson>().filter { p -> p.name == filter } }

  @SqlFragment
  fun people(filter: String) = sql { peopleNested(filter) }
}

fun main() {
  val result = q.build<PostgresDialect>().determinizeDynamics()

  println(q.determinizeDynamics().xr)
  println(result)
  println(result.debugData.phase.toString())
}
