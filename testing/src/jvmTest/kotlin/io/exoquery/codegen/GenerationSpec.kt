package io.exoquery.codegen

import io.exoquery.PostgresTestDB
import io.exoquery.sql
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.util.JdbcSchemaReader
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.toLowLevelConfig
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.sql.JDBCType

class GenerationSpec: FreeSpec({

  "should generate correct files from" - {
    "A local database" {
      val postgres = PostgresTestDB.embeddedPostgres
      val connectionMaker = { postgres.getPostgresDatabase().connection }
      val (config, propsData) =
        Code.Entities(
          tableFilter = "UserProfile",
          codeVersion = CodeVersion.Fixed("1.0.0"), // TODO need a test-generator for this
          driver = DatabaseDriver.Postgres(postgres.getJdbcUrl("postgres", "postgres") + "?search_path=purposely_inconsistent"),
          packagePrefix = "foo.bar"
        ).toLowLevelConfig("/my/drive", null) // todo specify a base-dir for properties

      val generator =
        JdbcGenerator.Test(
          config,
          JdbcSchemaReader({ JdbcSchemaReader.Conn(connectionMaker()) }, false),
          false,
        )
      generator.run()
      generator.fileWriter.getWrittenFiles().single().toContent() shouldBe
          CodeFileContent(
            "/my/drive/foo/bar/purposely_inconsistent/UserProfile.kt",
            "foo.bar.purposely_inconsistent",
            """
              @Serializable
              @SerialName("UserProfile")
              data class UserProfile(
                @SerialName("userId") val userId: Int,
                @SerialName("firstName") val firstName: String,
                val last_name: String,
                val updated_at: kotlinx.datetime.LocalDateTime?
              )
            """.trimIndent()
          )
    }

    "a simple mock schema" - {
      val schema =
        listOf(
          TableMock(
            name = "test_table",
            schema = "myschema",
            columns = listOf(
              ColumnMock("id", JDBCType.BIGINT, false),
              ColumnMock("first_name", JDBCType.VARCHAR, true)
            )
          )
        ).toSchema()

      "with various naming schemes" - {
        "with literal naming" {
          val (config, propsData) =
            Code.Entities(
              CodeVersion.Fixed("1.0.0"), // TODO need a test-generator for this
              DatabaseDriver.Postgres(),
              packagePrefix = "foo.bar",
              tableFilter = "test_table"
            ).toLowLevelConfig("/my/drive", null) // todo specify a base-dir for properties

          val generator = JdbcGenerator.Test(config, schema)
          generator.run()
          generator.fileWriter.getWrittenFiles().single().toContent() shouldBe
              CodeFileContent(
                "/my/drive/foo/bar/myschema/test_table.kt",
                "foo.bar.myschema",
                """
                @Serializable
                data class test_table(val id: Long, val first_name: String?)
                """.trimIndent()
              )
        }
        "with snake_case naming" {
          val (config, propsData) =
            Code.Entities(
              CodeVersion.Fixed("1.0.0"), // TODO need a test-generator for this
              DatabaseDriver.Postgres(),
              packagePrefix = "foo.bar",
              tableFilter = "test_table",
              nameParser = NameParser.SnakeCase
            ).toLowLevelConfig("/my/drive", null) // todo specify a base-dir for properties

          val generator = JdbcGenerator.Test(config, schema)
          generator.run()
          generator.fileWriter.getWrittenFiles().single().toContent() shouldBe
              CodeFileContent(
                "/my/drive/foo/bar/myschema/TestTable.kt",
                "foo.bar.myschema",
                """
                @Serializable
                @SerialName("test_table")
                data class TestTable(val id: Long, @SerialName("first_name") val firstName: String?)
              """.trimIndent()
              )
        }
      }
    }
  }

  // Need to test for the following case:
  // When there's a clause out of order e.g. Entities(
  //   CodeVersion.Fixed("1.0.0"),
  //   DatabaseDriver.Postgres("jdbc:postgresql://localhost:5432/postgres?search_path=public,purposely_inconsistent"),
  //   schemaFilter = "purposely_inconsistent",
  //   packagePrefix = "io.exoquery.example.schemaexample.content",
  // )
  // It shows up like this:
  // { // BLOCK
  //   val tmp0_codeVersion: Fixed = Fixed(version = "1.0.0")
  //   val tmp1_driver: Postgres = Postgres(jdbcUrl = "jdbc:postgresql://localhost:5432/postgres?search_path=public,purposely_inconsistent")
  //   Entities(codeVersion = tmp0_codeVersion, driver = tmp1_driver, packagePrefix = "io.exoquery.example.schemaexample.content", schemaFilter = "purposely_inconsistent")
  // }
  // We need to account for this structure by skipping the variables in the block and then looking them up later
  "should lookup vars when out of order" {
    sql.generateJustReturn(
      Code.Entities(
        CodeVersion.Fixed("1.0.0"),
        DatabaseDriver.Postgres("jdbc:postgresql://localhost:5432/postgres?search_path=public,purposely_inconsistent"),
        schemaFilter = "purposely_inconsistent",
        packagePrefix = "io.exoquery.example.schemaexample.content",
      )
    ) shouldBe Code.Entities(
      CodeVersion.Fixed("1.0.0"),
      DatabaseDriver.Postgres("jdbc:postgresql://localhost:5432/postgres?search_path=public,purposely_inconsistent"),
      schemaFilter = "purposely_inconsistent",
      packagePrefix = "io.exoquery.example.schemaexample.content",
    )
  }

})
