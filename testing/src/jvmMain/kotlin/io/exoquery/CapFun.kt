package io.exoqueryCapturedFunctionParamKinds

import io.exoquery.*
import io.exoquery.annotation.CapturedFunction
import io.exoquery.sql.PostgresDialect

data class MyPerson(val id: Long, val name: String)

val q = capture.select {
  val p = from(MyCaptureAheadObject.people("Jack"))
  p
}


object MyCaptureAheadObject {
  @CapturedFunction
  fun peopleNested(filter: String) = capture { Table<MyPerson>().filter { p -> p.name == filter } }

  @CapturedFunction
  fun people(filter: String) = capture { peopleNested(filter) }
}

fun main() {
  val result = q.build<PostgresDialect>().determinizeDynamics()

  println(q.determinizeDynamics().xr)
  println(result)
  println(result.debugData.phase.toString())
}
