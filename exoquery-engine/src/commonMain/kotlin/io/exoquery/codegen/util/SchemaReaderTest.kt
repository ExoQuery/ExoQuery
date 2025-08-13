package io.exoquery.codegen.util

import io.exoquery.codegen.model.ColumnMeta
import io.exoquery.codegen.model.DatabaseTypes
import io.exoquery.codegen.model.TableMeta
import kotlin.jvm.JvmInline

/**
 * A test implementation of [SchemaReader] that allows you to provide predefined tables and columns.
 * This is useful for testing code generation without needing a real database connection.
 * TODO move to a test module
 */
class SchemaReaderTest(
  val testSchema: TestSchema,
  override val allowUnknownDatabase: Boolean
): SchemaReader() {
  override fun makeConnection(): SchemaReader.Conn =
    Conn(testSchema.tables, testSchema.columns, testSchema.databaseType)

  @JvmInline
  value class ResultsTableMeta(private val iterator: Iterator<TableMeta>) : SchemaReader.ResultsTableMeta {
    override fun next(): Boolean = iterator.hasNext()
    override fun makeTableMeta(): TableMeta = iterator.next()
  }
  @JvmInline
  value class ResultsColumnMeta(private val iterator: Iterator<ColumnMeta>) : SchemaReader.ResultsColumnMeta {
    override fun next(): Boolean = iterator.hasNext()
    override fun makeColumnMeta(): ColumnMeta = iterator.next()
  }

  class Conn(val tables: List<TableMeta>, val columns: List<ColumnMeta>, val knownDatabaseType: DatabaseTypes.DatabaseType) : SchemaReader.Conn {
    override fun getTableData(schemaPattern: String?): SchemaReader.ResultsTableMeta =
      ResultsTableMeta(tables.iterator())

    override fun getColumnData(schemaPattern: String?): SchemaReader.ResultsColumnMeta =
      ResultsColumnMeta(columns.iterator())

    override fun getSchema(): String? = null
    override fun getDatabaseType(): DatabaseTypes.DatabaseType = knownDatabaseType
    override fun close() {}
  }

  data class TestSchema(
    val tables: List<TableMeta>,
    val columns: List<ColumnMeta>,
    val databaseType: DatabaseTypes.DatabaseType
  )
}
