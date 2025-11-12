package io.exoquery.debug

import io.exoquery.sql


data class MyPerson(val id: Int, val name: String)
data class MyValue(val value: Int, val description: String)


// TODO NEED TO ADD THIS TO TESTS
fun a() {
  val ids = listOf(1, 2, 3)
  val people = sql {
    Table<MyPerson>().filter { p -> p.id in params(ids) }
  }
  println(people.buildFor.Postgres().value)
}

fun b() {
  val people = sql {
    Table<MyPerson>().filter { p -> p.id in Table<MyValue>().map { v -> v.value } }
  }
  println(people.buildFor.Postgres().value)
}

fun main() {
  a()
  b()
}

// Ugly error, need to think about fixing this
//"correlated contains - subquery" {
//  val people = sql {
//    val subquery = sql {
//      Table<Person>().map { p -> p.name }
//    }
//    Table<Person>().filter { p -> p.name in subquery }
//  }
//  shouldBeGolden(people.xr, "XR")
//  shouldBeGolden(people.build<PostgresDialect>())
//}
