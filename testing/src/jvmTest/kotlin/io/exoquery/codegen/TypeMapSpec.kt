package io.exoquery.codegen

import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.toLowLevelConfig
import io.exoquery.generation.typemap.ClassOf
import io.exoquery.generation.typemap.From
import io.exoquery.generation.typemap.TypeMap
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.sql.JDBCType

class TypeMapSpec: FreeSpec({
  val schema = listOf(
    TableMock(
      name = "TestTable",
      schema = "myschema",
      columns = listOf(
        ColumnMock("id", JDBCType.BIGINT, false),
        ColumnMock("firstName", JDBCType.VARCHAR, true)
      )
    )
  ).toSchema()

  fun makeWithTypeMap(typeMap: TypeMap) =
    Code.DataClasses(
      CodeVersion.Fixed("1.0.0"),
      DatabaseDriver.Postgres(),
      packagePrefix = "foo.bar",
      typeMap = typeMap
    ).toLowLevelConfig("/my/drive", null).first

  "should map types based on" - {
    "column name" {
      val config = makeWithTypeMap(
        TypeMap(
          From(column = "firstName") to ClassOf("kotlin.Double"),
        )
      )
      val generator = JdbcGenerator.Test(config, schema)
      generator.run()
      generator.fileWriter.getWrittenFiles().single().toContent() shouldBe
          CodeFileContent(
            "/my/drive/foo/bar/myschema/TestTable.kt",
            "foo.bar.myschema",
            "data class TestTable(val id: Long, val firstName: Double?)"
          )
    }
    "column type" {
      val config = makeWithTypeMap(
        TypeMap(
          From(typeName = "varchar") to ClassOf("kotlin.Double"),
        )
      )
      val generator = JdbcGenerator.Test(config, schema)
      generator.run()
      generator.fileWriter.getWrittenFiles().single().toContent() shouldBe
          CodeFileContent(
            "/my/drive/foo/bar/myschema/TestTable.kt",
            "foo.bar.myschema",
            "data class TestTable(val id: Long, val firstName: Double?)"
          )
    }

    "column type number" {
      val config = makeWithTypeMap(
        TypeMap(
          From(typeNum = JDBCType.VARCHAR.vendorTypeNumber) to ClassOf("kotlin.Double"),
        )
      )
      val generator = JdbcGenerator.Test(config, schema)
      generator.run()
      generator.fileWriter.getWrittenFiles().single().toContent() shouldBe
          CodeFileContent(
            "/my/drive/foo/bar/myschema/TestTable.kt",
            "foo.bar.myschema",
            "data class TestTable(val id: Long, val firstName: Double?)"
          )
    }

    "column name and type" {
      val schema = listOf(
        TableMock(
          name = "TestTable",
          schema = "myschema",
          columns = listOf(
            ColumnMock("id", JDBCType.BIGINT, false),
            ColumnMock("firstName", JDBCType.VARCHAR, true),
            ColumnMock("firstName", JDBCType.BIGINT, false) // normally in jdbc 2 columns can't have same name but here we have it just to test that both conditions are needed
          )
        )
      ).toSchema()

      val config = makeWithTypeMap(
        TypeMap(
          From(column = "firstName", typeName = "varchar") to ClassOf("kotlin.Double")
        )
      )
      val generator = JdbcGenerator.Test(config, schema)
      generator.run()
      generator.fileWriter.getWrittenFiles().single().toContent() shouldBe
          CodeFileContent(
            "/my/drive/foo/bar/myschema/TestTable.kt",
            "foo.bar.myschema",
            "data class TestTable(val id: Long, val firstName: Double?, val firstName: Long)"
          )
    }
    "column name and table name" {
      val schema = listOf(
        TableMock(
          name = "TestTable",
          schema = "myschema",
          columns = listOf(
            ColumnMock("id", JDBCType.BIGINT, false),
            ColumnMock("firstName", JDBCType.VARCHAR, true),
          )
        ),
        TableMock(
          name = "AnotherTestTable",
          schema = "myschema",
          columns = listOf(
            ColumnMock("id", JDBCType.BIGINT, false),
            ColumnMock("firstName", JDBCType.VARCHAR, true),
          )
        )
      ).toSchema()

      val config = makeWithTypeMap(
        TypeMap(
          From(column = "firstName", table = "TestTable") to ClassOf("kotlin.Double")
        )
      )

      val generator = JdbcGenerator.Test(config, schema)
      generator.run()
      generator.fileWriter.getWrittenFiles().map { it.toContent() }.toSet() shouldBe
          setOf(
            CodeFileContent(
              "/my/drive/foo/bar/myschema/TestTable.kt",
              "foo.bar.myschema",
              "data class TestTable(val id: Long, val firstName: Double?)"
            ),
            CodeFileContent(
              "/my/drive/foo/bar/myschema/AnotherTestTable.kt",
              "foo.bar.myschema",
              "data class AnotherTestTable(val id: Long, val firstName: String?)"
            )
          )

    }
    "column name, table name and schema" {
      val schema = listOf(
        TableMock(
          name = "TestTable",
          schema = "myschema",
          columns = listOf(
            ColumnMock("id", JDBCType.BIGINT, false),
            ColumnMock("firstName", JDBCType.VARCHAR, true),
          )
        ),
        TableMock(
          name = "TestTable",
          schema = "another_schema",
          columns = listOf(
            ColumnMock("id", JDBCType.BIGINT, false),
            ColumnMock("firstName", JDBCType.VARCHAR, true),
          )
        )
      ).toSchema()

      val config = makeWithTypeMap(
        TypeMap(
          From(column = "firstName", table = "TestTable", schema = "myschema") to ClassOf("kotlin.Double")
        )
      )

      val generator = JdbcGenerator.Test(config, schema)
      generator.run()
      generator.fileWriter.getWrittenFiles().map { it.toContent() }.toSet() shouldBe
          setOf(
            CodeFileContent(
              "/my/drive/foo/bar/myschema/TestTable.kt",
              "foo.bar.myschema",
              "data class TestTable(val id: Long, val firstName: Double?)"
            ),
            CodeFileContent(
              "/my/drive/foo/bar/another_schema/TestTable.kt",
              "foo.bar.another_schema",
              "data class TestTable(val id: Long, val firstName: String?)"
            )
          )
    }
  }
})
