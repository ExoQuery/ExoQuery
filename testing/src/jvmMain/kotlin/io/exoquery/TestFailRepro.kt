package io.exoquery

object TestFailRepro {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val ownerId: Int, val street: String, val city: String)

  val people = sql { Table<Person>() }
  val addresses = sql { Table<Address>() }

  val c = sql {
    select {
      val p = from(people)
      val a = joinLeft(addresses) { it.ownerId == p.id }
      where { p.age > 18 }
      groupBy(p)
      p
    }.filter { ccc -> ccc.name == "Main St" }
  }
}

fun main() {
  val reproQuery = TestFailRepro.c
  println(reproQuery.build<PostgresDialect>().value)
}
