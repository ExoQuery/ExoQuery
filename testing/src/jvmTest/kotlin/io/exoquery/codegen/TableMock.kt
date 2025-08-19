package io.exoquery.codegen

import io.exoquery.codegen.model.ColumnMeta
import io.exoquery.codegen.model.DatabaseTypes
import io.exoquery.codegen.model.TableMeta
import io.exoquery.codegen.util.SchemaReaderTest
import java.sql.DatabaseMetaData
import java.sql.JDBCType

data class TableMock(
  val name: String,
  val schema: String,
  val columns: List<ColumnMock>
) {
  fun toTableMeta(): TableMeta =
    TableMeta(
      tableCat = null,
      tableName = name,
      tableType = "TABLE",
      tableSchema = schema
    )
  fun toColumnMetas(): List<ColumnMeta> =
    columns.map {
      ColumnMeta(
        tableCat = null,
        tableSchema = schema,
        tableName = name,
        columnName = it.name,
        dataType = it.typeNum,
        typeName = "UNUSED",
        nullable = if (it.nullable) DatabaseMetaData.columnNullable else DatabaseMetaData.columnNoNulls,
        size = 0 // UNUSED
      )
    }
}


data class ColumnMock(val name: String, val typeNum: Int, val nullable: Boolean) {
  constructor(
    name: String,
    type: JDBCType,
    nullable: Boolean = true
  ) : this(name, typeNum = type.vendorTypeNumber, nullable = nullable)
}

fun List<TableMock>.toSchema(): SchemaReaderTest.TestSchema =
  SchemaReaderTest.TestSchema(
    tables = this.map { it.toTableMeta() },
    columns = this.flatMap { it.toColumnMetas() },
    databaseType = DatabaseTypes.Postgres
  )
