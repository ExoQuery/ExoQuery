package io.exoquery


fun main() {
  data class Person(val id: Int, val name: String, val age: Int)

  val cap =
    sql {
      Table<Person>().filter { p -> p.name == "Joe" }
    }

  println("----------------- XR ---------------\n" + cap.xr.showRaw())
  val built = cap.buildFor.Postgres()
  //val built = cap.buildRuntime(PostgresDialect(), null, true)
  println("----------------- SQL ---------------\n" + built.value)
}
