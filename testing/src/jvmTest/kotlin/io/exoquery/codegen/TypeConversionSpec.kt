package io.exoquery.codegen

import io.exoquery.codegen.model.CodeGenerationError
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.UnrecognizedTypeStrategy
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.toLowLevelConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.sql.JDBCType

class TypeConversionSpec: FreeSpec({
  "an unrecognized type strategy" - {
    val schema =
      listOf(
        TableMock(
          name = "test_table",
          schema = "myschema",
          columns = listOf(
            ColumnMock("id", 1000, false),
            ColumnMock("first_name", JDBCType.VARCHAR, true)
          )
        )
      ).toSchema()

    "should assume string" {
      val (config, _) =
        Code.DataClasses(
          CodeVersion.Fixed("1.0.0"), // TODO need a test-generator for this
          DatabaseDriver.Postgres(),
          packagePrefix = "foo.bar",
          tableFilter = "test_table",
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.AssumeString
        ).toLowLevelConfig("/my/drive", null) // todo specify a base-dir for properties

      val generator = JdbcGenerator.Test(config, schema)
      generator.run()
      generator.fileWriter.getWrittenFiles().single().toContent() shouldBe
        CodeFileContent(
          "/my/drive/foo/bar/myschema/test_table.kt",
          "foo.bar.myschema",
          """
          @Serializable
          data class test_table(val id: String, val first_name: String?)
          """.trimIndent()
        )
    }
    "should throw typing error" {
      val (config, propsData) =
        Code.DataClasses(
          CodeVersion.Fixed("1.0.0"), // TODO need a test-generator for this
          DatabaseDriver.Postgres(),
          packagePrefix = "foo.bar",
          tableFilter = "test_table",
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.ThrowTypingError
        ).toLowLevelConfig("/my/drive", null) // todo specify a base-dir for properties

      val generator = JdbcGenerator.Test(config, schema)
      shouldThrow<CodeGenerationError> {
        generator.run()
      }
    }
    "should skip column" {
      val (config, _) =
        Code.DataClasses(
          CodeVersion.Fixed("1.0.0"), // TODO need a test-generator for this
          DatabaseDriver.Postgres(),
          packagePrefix = "foo.bar",
          tableFilter = "test_table",
          unrecognizedTypeStrategy = UnrecognizedTypeStrategy.SkipColumn
        ).toLowLevelConfig("/my/drive", null) // todo specify a base-dir for properties

      val generator = JdbcGenerator.Test(config, schema)
      generator.run()
      generator.fileWriter.getWrittenFiles().single().toContent() shouldBe
        CodeFileContent(
          "/my/drive/foo/bar/myschema/test_table.kt",
          "foo.bar.myschema",
          """
          @Serializable
          data class test_table(val first_name: String?)
          """.trimIndent()
        )
    }
  }
})
