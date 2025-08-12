package io.exoquery

import io.exoquery.generation.Code
import io.exoquery.generation.DatabaseDriver
import io.exoquery.kmp.pprint

fun main() {
  //val src = printSource {
  //  Code.DataClasses(
  //    "1.1",
  //    DatabaseDriver.Postgres,
  //    propertiesFile = PropertiesFile.Custom("foobar")
  //  )
  //}
  //println(src)

  val cc = capture.generateAndReturn(
    Code.DataClasses(
      "1.1",
      DatabaseDriver.Postgres("jdbc:postgresql://localhost:5432/postgres"),
      packagePrefix = "io.exoquery",
      username = "postgres",
      password = "postgres"
    )
  )


  println(pprint(cc))
}
