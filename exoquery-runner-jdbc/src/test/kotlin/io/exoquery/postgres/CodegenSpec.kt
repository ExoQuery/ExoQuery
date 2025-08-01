package io.exoquery.postgres

import io.exoquery.TestDatabases
import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.WorkingDir
import io.kotest.core.spec.style.FreeSpec

class CodegenSpec : FreeSpec({
  val ctx = TestDatabases.postgres

  val codegen = JdbcGenerator(
    LowLevelCodeGeneratorConfig(BasicPath.WorkingDir() + "gen", BasicPath.DotPath("io.exoquery")),
    { ctx.database.connection }
  )

  "should generate files" {
    val deliverable = codegen.compute()
    deliverable.files.forEach { file ->
      println("Writing file: ${file.fullPath()}")
      file.write()
    }
  }
})
