package io.exoquery.sample

import io.exoquery.sql

fun main() {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val ownerId: Int, val street: String)

  val q = sql.select {
    val p = from(Table<Person>())
    val a = join(Table<Address>()) { a -> a.ownerId == p.id }
    groupBy(p.name) // comment this out and error should result
    having { avg(p.age) < 50 }
    p.name to avg(p.age)
  }

  println("============= Before Transform ==============\n" + q.show())
  println("============= After Transform ==============\n" + q.normalizeSelects().show())
  println("=========== Query Value ===============\n" + q.buildPrettyFor.Postgres().value)
}
