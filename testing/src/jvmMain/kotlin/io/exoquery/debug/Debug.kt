package io.exoquery.debug

import io.exoquery.SqlQuery
import io.exoquery.capture
import io.exoquery.printSource
import io.exoquery.printSourceBefore

data class MyPerson(val name: String)

object BackObject {
  val people = capture.select {
    val p = from(Table<MyPerson>())
    p
  }
}



fun main() {
  //val src = printSourceBefore {
  //  capture.select {
  //    val x = from(BackObject.people)
  //    x
  //  }
  //}
  //println(src)

  //val cap = capture.select {
  //  val x = from(BackObject.people)
  //  x
  //}
  //println(cap.buildFor.Postgres())
}
