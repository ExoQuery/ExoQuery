package io.exoquery.codegen.util

import io.exoquery.codegen.model.ColumnMeta
import io.exoquery.codegen.model.DatabaseTypes
import io.exoquery.codegen.model.RawMeta
import io.exoquery.codegen.model.TableMeta

abstract class SchemaReader<Conn: AutoCloseable, Results> {

  protected abstract fun Results.next(): Boolean
  protected abstract fun Results.makeTableMeta(): TableMeta
  protected abstract fun Results.makeColumnMeta(): ColumnMeta
  protected abstract fun Conn.getTableData(schemaPattern: String?): Results
  protected abstract fun Conn.getColumnData(schemaPattern: String?): Results
  protected fun Conn.getSchema(): String? = null // Only really need this for Oracle because very slow without specifying a schema
  abstract fun getDatabaseType(conn: Conn): DatabaseTypes.DatabaseType

  protected fun <T> resultSetExtractor(rs: Results, extractor: (Results) -> T): List<T> {
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
    val databaseType = getDatabaseType(conn)
    val tables = extractTables(conn, databaseType)
    val columns = extractColumns(conn, databaseType)
    Triple(tables, columns, databaseType)
  }



  operator fun invoke(connectionMaker: () -> Conn): Pair<List<RawMeta>, DatabaseTypes.DatabaseType> {
    val (tables, columns, databaseType) = connectionMaker().use { conn ->
      extractTablesAndColumns(conn)
    }
    val tableMap = tables.associateBy { Triple(it.tableCat, it.tableSchema, it.tableName) }
    val columnMap =
      columns
      .groupBy { Triple(it.tableCat, it.tableSchema, it.tableName) }
      .mapNotNull { (key, cols) ->
        tableMap[key]?.let { table -> RawMeta(table, cols) }
      }
    return columnMap to databaseType
  }


  //  override def apply(connectionMaker: JdbcConnectionMaker): Seq[RawSchema[JdbcTableMeta, JdbcColumnMeta]] = {
//    val tableMap =
//      extractTables(connectionMaker)
//        .map(t => ((t.tableCat, t.tableSchema, t.tableName), t))
//        .toMap
//
//    val columns = extractColumns(connectionMaker)
//    val tableColumns =
//      columns
//        .groupBy(c => (c.tableCat, c.tableSchema, c.tableName))
//        .map { case (tup, cols) => tableMap.get(tup).map(RawSchema(_, cols)) }
//        .collect { case Some(tbl) => tbl }
//
//    tableColumns.toSeq
//  }
}

//class DefaultJdbcSchemaReader(
//  databaseType: DatabaseType
//) extends JdbcSchemaReader {
//
//  @tailrec
//  private def resultSetExtractor[T](rs: ResultSet, extractor: (ResultSet) => T, acc: List[T] = List.empty): List[T] =
//    if (!rs.next())
//      acc.reverse
//    else
//      resultSetExtractor(rs, extractor, extractor(rs) :: acc)
//
//  private[getquill] def schemaPattern(schema: String): String =
//    databaseType match {
//      case Oracle => schema // Oracle meta fetch takes minutes to hours if schema is not specified
//      case _      => null
//    }
//
//  def jdbcEntityFilter(ts: JdbcTableMeta): Boolean =
//    ts.tableType.existsInSetNocase("table", "view", "user table", "user view", "base table")
//
//  private[getquill] def extractTables(connectionMaker: () => Connection): List[JdbcTableMeta] = {
//    val output = Using.Manager { use =>
//      val conn   = use(connectionMaker())
//      val schema = conn.getSchema
//      val rs = use {
//        conn.getMetaData.getTables(
//          null,
//          schemaPattern(schema),
//          null,
//          null
//        )
//      }
//      resultSetExtractor(rs, rs => JdbcTableMeta.fromResultSet(rs))
//    }
//    val unfilteredJdbcEntities =
//      output match {
//        case Success(value) => value
//        case Failure(e)     => throw e
//      }
//
//    unfilteredJdbcEntities.filter(jdbcEntityFilter)
//  }
//
//  private[getquill] def extractColumns(connectionMaker: () => Connection): List[JdbcColumnMeta] = {
//    val output = Using.Manager { use =>
//      val conn   = use(connectionMaker())
//      val schema = conn.getSchema
//      val rs = use {
//        conn.getMetaData.getColumns(
//          null,
//          schemaPattern(schema),
//          null,
//          null
//        )
//      }
//      resultSetExtractor(rs, rs => JdbcColumnMeta.fromResultSet(rs))
//    }
//    output match {
//      case Success(value) => value
//      case Failure(e)     => throw e
//    }
//  }
//
//}
