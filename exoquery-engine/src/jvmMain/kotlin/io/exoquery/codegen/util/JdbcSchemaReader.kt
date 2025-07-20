package io.exoquery.codegen.util

import io.exoquery.codegen.model.ColumnMeta
import io.exoquery.codegen.model.DatabaseTypes
import io.exoquery.codegen.model.JdbcMetaMakers
import io.exoquery.codegen.model.JdbcTypeInfo
import io.exoquery.codegen.model.TableMeta
import java.sql.Connection
import java.sql.ResultSet

data class JdbcSchemaReader(val allowUnknownDatabase: Boolean): SchemaReader<Connection, ResultSet>() {
  override fun ResultSet.next(): Boolean = this.next()
  override fun ResultSet.makeTableMeta(): TableMeta = JdbcMetaMakers.makeTableMeta(this)
  override fun ResultSet.makeColumnMeta(): ColumnMeta = JdbcMetaMakers.makeColumnMeta(this)

  override fun Connection.getTableData(schemaPattern: String?): ResultSet =
    this.metaData.getTables(null, schemaPattern, null, null)

  override fun Connection.getColumnData(schemaPattern: String?): ResultSet =
    this.metaData.getColumns(null, schemaPattern, null, null)

  override fun getDatabaseType(conn: Connection): DatabaseTypes.DatabaseType =
    try {
      DatabaseTypes.fromProductName(conn.metaData.databaseProductName)
    } catch (e: Exception) {
      if (allowUnknownDatabase) {
        DatabaseTypes.Unknown(conn.metaData.databaseProductName)
      } else throw IllegalStateException(
        "Failed to determine database type from product name: ${conn.metaData.databaseProductName}",
        e
      )
    }
}
