package io.exoquery.jdbc.postgres

import io.exoquery.jdbc.TestDatabases
import io.kotest.core.spec.style.FreeSpec

class CodegenSpec : FreeSpec({
  val ctx = TestDatabases.postgres

  //sql.generate(
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
