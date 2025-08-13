package io.exoquery.codegen

import io.exoquery.codegen.model.ColumnMeta
import io.exoquery.codegen.model.DatabaseTypes
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.TableMeta
import io.exoquery.codegen.util.SchemaReaderTest
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.toLowLevelConfig
import io.kotest.core.spec.style.FreeSpec
import java.sql.DatabaseMetaData
import java.sql.JDBCType

data class TableMock(
  val name: String,
  val cat: String,
  val columns: List<ColumnMock>
) {
  fun toTableMeta(): TableMeta =
    TableMeta(
      tableCat = cat,
      tableName = name,
      tableType = "TABLE",
      tableSchema = null
    )
  fun toColumnMetas(): List<ColumnMeta> =
    columns.map {
      ColumnMeta(
        tableCat = cat,
        tableSchema = null,
        tableName = name,
        columnName = it.name,
        dataType = it.type.ordinal,
        typeName = "UNUSED",
        nullable = if (it.nullable) DatabaseMetaData.columnNoNulls else DatabaseMetaData.columnNullable,
        size = 0 // UNUSED
      )
    }
}
data class ColumnMock(
  val name: String,
  val type: JDBCType,
  val nullable: Boolean
)

fun List<TableMock>.toSchema(): SchemaReaderTest.TestSchema =
  SchemaReaderTest.TestSchema(
    tables = this.map { it.toTableMeta() },
    columns = this.flatMap { it.toColumnMetas() },
    databaseType = DatabaseTypes.Postgres
  )


class GenerationSpec: FreeSpec({

  "should generate correct files from" - {
    "one simple table" {

      val (config, propsData) =
        Code.DataClasses(
          CodeVersion.Fixed("1.0.0"), // TODO need a test-generator for this
          DatabaseDriver.Postgres(),
          packagePrefix = "foo.bar"
        ).toLowLevelConfig("/my/drive", null) // todo specify a base-dir for properties

      val schema =
        listOf(
          TableMock(
            name = "test_table",
            cat = "public",
            columns = listOf(
              ColumnMock("id", JDBCType.INTEGER, false),
              ColumnMock("first_name", JDBCType.VARCHAR, true)
            )
          )
        ).toSchema()

      val generator = JdbcGenerator.Test(schema, config)
      generator.run()

      val writtenFiles = generator.fileWriter.getWrittenFiles()
      writtenFiles.forEach { println(it) }
    }
  }

})
