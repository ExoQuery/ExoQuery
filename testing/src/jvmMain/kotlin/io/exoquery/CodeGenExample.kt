package io.exoquery

import io.exoquery.generation.Code
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.PropertiesFile
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
      DatabaseDriver.Postgres,
      propertiesFile = PropertiesFile.Custom("foobar")
    )
  )


  println(pprint(cc))
}
