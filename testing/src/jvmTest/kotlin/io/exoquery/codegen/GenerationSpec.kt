package io.exoquery.codegen

import io.exoquery.codegen.model.DatabaseTypes
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.util.SchemaReaderTest
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.toLowLevelConfig
import io.kotest.core.spec.style.FreeSpec

class GenerationSpec: FreeSpec({
  "should generate correct files from" - {
    "one simple table" {

      val (config, propsData) =
        Code.DataClasses(
          CodeVersion.Fixed("1.0.0"), // TODO need a test-generator for this
          DatabaseDriver.Postgres(),
          packagePrefix = "foo.bar"
        ).toLowLevelConfig("/my/drive", null) // todo specify a base-dir for properties

      val schema = SchemaReaderTest.TestSchema(
        listOf(), listOf(), DatabaseTypes.Postgres
      )

      JdbcGenerator.Test(schema, config)

    }
  }

})
