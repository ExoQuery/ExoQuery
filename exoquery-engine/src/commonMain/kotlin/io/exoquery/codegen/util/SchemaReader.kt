package io.exoquery.codegen.util

import io.exoquery.codegen.model.ColumnMeta
import io.exoquery.codegen.model.DatabaseTypes
import io.exoquery.codegen.model.RawTableMeta
import io.exoquery.codegen.model.TableMeta

abstract class SchemaReader {
  /**
   * Encapsulates a result-set that returns table metadata
   * this was a undesired but necessary abstraction in order to be able to load test
   * schemas into JdbcGenerator without having to mock JDBC ResultSet and other components.
   * Also, in order to differentiate between table-meta and column-meta result sets
   * I decided to have two different interfaces, this ensures that one is not
   * accidentally swapped with the other.
   */
  interface ResultsTableMeta {
    fun next(): Boolean
    fun makeTableMeta(): TableMeta
  }
  interface ResultsColumnMeta {
    fun next(): Boolean
    fun makeColumnMeta(): ColumnMeta
  }
  interface Conn: AutoCloseable {
    fun getTableData(schemaPattern: String?): ResultsTableMeta
    fun getColumnData(schemaPattern: String?): ResultsColumnMeta
    fun getSchema(): String? = null // Only really need this for Oracle because very slow without specifying a schema
    fun getDatabaseType(): DatabaseTypes.DatabaseType
  }

  abstract val allowUnknownDatabase: Boolean
  abstract fun makeConnection(): Conn

  fun readSchemas(): Pair<List<RawTableMeta>, DatabaseTypes.DatabaseType> {
    val (tables, columns, databaseType) = makeConnection().use { conn ->
      extractTablesAndColumns(conn)
    }
    val tableMap = tables.associateBy { Triple(it.tableCat, it.tableSchema, it.tableName) }
    val columnMap =
      columns
        .groupBy { Triple(it.tableCat, it.tableSchema, it.tableName) }
        .mapNotNull { (key, cols) ->
          tableMap[key]?.let { table -> RawTableMeta(table, cols) }
        }
    return columnMap to databaseType
  }

  // Since I decided to have two different types for results in order to not to confuse results that return table metas
  // with those that return column metas this needs to be duplicated. Could unify with a parent ResultSet class but not worth
  // the additional semantic complexity for now.
  protected fun <T> resultSetExtractor(rs: ResultsTableMeta, extractor: (ResultsTableMeta) -> T): List<T> {
    val results = mutableListOf<T>()
    while (rs.next()) {
      results.add(extractor(rs))
    }
    return results
  }
  protected fun <T> resultSetExtractor(rs: ResultsColumnMeta, extractor: (ResultsColumnMeta) -> T): List<T> {
    val results = mutableListOf<T>()
    while (rs.next()) {
      results.add(extractor(rs))
    }
    return results
  }

  private fun schemaPattern(schema: String?, databaseType: DatabaseTypes.DatabaseType): String? = null
    // When oracle is introduced need to have this:
    // when (DatabaseTypes.current) {
    //  DatabaseTypes.Oracle -> schema // Oracle meta fetch takes minutes to hours if schema is not specified
    //  else -> null
    //}

  private fun entityFilter(ts: TableMeta): Boolean =
    ts.tableType?.inSetNocase("table", "view", "user table", "user view", "base table") ?: false

  private fun extractTables(conn: Conn, databaseType: DatabaseTypes.DatabaseType): List<TableMeta> {
    val output = run {
      val schema = conn.getSchema()
      val rs = conn.getTableData(
        schemaPattern(schema, databaseType)
      )
      resultSetExtractor(rs) { rs.makeTableMeta() }
    }
    return output.filter(::entityFilter)
  }

  private fun extractColumns(conn: Conn, databaseType: DatabaseTypes.DatabaseType): List<ColumnMeta> {
    val output = run {
      val schema = conn.getSchema()
      val rs = conn.getColumnData(
        schemaPattern(schema, databaseType)
      )
      resultSetExtractor(rs) { rs.makeColumnMeta() }
    }
    return output
  }

  private fun extractTablesAndColumns(conn: Conn): Triple<List<TableMeta>, List<ColumnMeta>, DatabaseTypes.DatabaseType> = run {
    val databaseType = conn.getDatabaseType()
    if (databaseType is DatabaseTypes.Unknown && !allowUnknownDatabase) {
      throw IllegalStateException(
        "Failed to determine database type from product name: ${databaseType.databaseName}"
      )
    }

    val tables = extractTables(conn, databaseType)
    val columns = extractColumns(conn, databaseType)
    Triple(tables, columns, databaseType)
  }
}
