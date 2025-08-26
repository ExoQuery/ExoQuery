package io.exoquery.postgres

import io.exoquery.TestDatabases
import io.kotest.core.spec.style.FreeSpec

class CodegenSpec : FreeSpec({
  val ctx = TestDatabases.postgres

  //capture.generate(
  //  Code.Entities(
  //    "v1",
  //    DatabaseDriver.Postgres(),
  //    packagePrefix = "io.exoquery",
  //    password = "postgres",
  //    username = "postgres"
  //  )
  //)

//  val codegen = JdbcGenerator(
//    LowLevelCodeGeneratorConfig(BasicPath.WorkingDir() + "gen", BasicPath.DotPath("io.exoquery")),
//    { ctx.database.connection }
//  )
//
//  "should generate files" {
//    val deliverable = codegen.compute()
//    deliverable.files.forEach { file ->
//      file.deliverable.tables.forEach { println(it.name) }
//    }
//  }
})
