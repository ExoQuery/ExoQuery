package io.exoquery.codegen.util

import io.exoquery.codegen.model.CodeGenerationError
import io.exoquery.codegen.model.ColumnMeta
import io.exoquery.codegen.model.DatabaseTypes
import io.exoquery.codegen.model.JdbcMetaMakers
import io.exoquery.codegen.model.JdbcTypeInfo
import io.exoquery.codegen.model.TableMeta
import java.sql.Connection
import java.sql.ResultSet

data class JdbcSchemaReader(val connectionMaker: () -> SchemaReader.Conn, override val allowUnknownDatabase: Boolean): SchemaReader() {
  @JvmInline
  value class ResultsTableMeta(val rs: ResultSet): SchemaReader.ResultsTableMeta {
    override fun next(): Boolean = rs.next()
    override fun makeTableMeta(): TableMeta = JdbcMetaMakers.makeTableMeta(rs)
  }
  @JvmInline
  value class ResultsColumnMeta(val rs: ResultSet): SchemaReader.ResultsColumnMeta {
    override fun next(): Boolean = rs.next()
    override fun makeColumnMeta(): ColumnMeta = JdbcMetaMakers.makeColumnMeta(rs)
  }

  @JvmInline
  value class Conn(val connection: Connection): SchemaReader.Conn {
    override fun getTableData(schemaPattern: String?): SchemaReader.ResultsTableMeta =
      ResultsTableMeta(connection.metaData.getTables(null, schemaPattern, null, null))

    override fun getColumnData(schemaPattern: String?): SchemaReader.ResultsColumnMeta =
      ResultsColumnMeta(connection.metaData.getColumns(null, schemaPattern, null, null))

    override fun close() = connection.close()

    override fun getDatabaseType(): DatabaseTypes.DatabaseType =
      try {
        DatabaseTypes.fromProductName(connection.metaData.databaseProductName)
      } catch (e: Exception) {
        DatabaseTypes.Unknown(connection.metaData.databaseProductName)
      }
  }


  override fun makeConnection(): SchemaReader.Conn =
    try {
      connectionMaker()
    } catch (e: Exception) {
      throw CodeGenerationError("Code Generation Failed. JdbcGenerator Failed to make a connection using the provided connection maker: ${e.message}", e)
    }
}
