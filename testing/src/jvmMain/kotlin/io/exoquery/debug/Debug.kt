package io.exoquery.debug

import io.exoquery.capture

data class MyPerson(val name: String)

fun main() {
  val people = capture.select {
    val p = from(Table<MyPerson>())
    p
  }
  println(people.buildFor.Postgres().value)
}
