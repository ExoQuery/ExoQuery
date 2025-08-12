package io.exoquery

import io.exoquery.codegen.model.LLM
import io.exoquery.codegen.model.NameParser
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
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
      CodeVersion.Fixed("1.5"),
      DatabaseDriver.Postgres("jdbc:postgresql://localhost:5432/postgres"),
      packagePrefix = "io.exoquery",
      username = "postgres",
      password = "postgres",
      nameParser =
        NameParser.Composite(
          NameParser.UsingLLM(
            LLM.OpenAI()
          ),
          NameParser.UncapitalizeColumns
        ),
      detailedLogs = true
    )
  )


  println(pprint(cc))
}
