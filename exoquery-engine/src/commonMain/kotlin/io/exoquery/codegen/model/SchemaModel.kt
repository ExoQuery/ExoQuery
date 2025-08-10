package io.exoquery.codegen.model



data class RawTableMeta(
  val table: TableMeta,
  val columns: List<ColumnMeta>
)

data class TableMeta(
  val tableCat: String?,
  val tableSchema: String?,
  val tableName: String,
  val tableType: String?
)

data class ColumnMeta(
  val tableCat: String?,
  val tableSchema: String?,
  val tableName: String,
  val columnName: String,
  val dataType: Int,
  val typeName: String,
  val nullable: Int,
  val size: Int
)

//case class RawSchema[T, C](table: T, columns: Seq[C])
//
//
//case class TableMeta(
//  tableCat: Option[String],
//  tableSchema: Option[String],
//  tableName: String,
//  tableType: Option[String]
//)
// TODO move into the JVM-specific codegen module
//object TableMeta {
//  def fromResultSet(rs: ResultSet) = TableMeta(
//    tableCat = Option(rs.getString("TABLE_CAT")),
//    tableSchema = Option(rs.getString("TABLE_SCHEM")),
//    tableName = rs.getString("TABLE_NAME"),
//    tableType = Option(rs.getString("TABLE_TYPE"))
//  )
//}
//
//case class ColumnMeta(
//  tableCat: Option[String],
//  tableSchema: Option[String],
//  tableName: String,
//  columnName: String,
//  dataType: Int,
//  typeName: String,
//  nullable: Int,
//  size: Int
//) extends BasicColumnMeta
//
// TODO move into the JVM-specific codegen module
//object ColumnMeta {
//  def fromResultSet(rs: ResultSet) =
//    ColumnMeta(
//      tableCat = Option(rs.getString("TABLE_CAT")),
//      tableSchema = Option(rs.getString("TABLE_SCHEM")),
//      tableName = rs.getString("TABLE_NAME"),
//      columnName = rs.getString("COLUMN_NAME"),
//      dataType = rs.getInt("DATA_TYPE"),
//      typeName = rs.getString("TYPE_NAME"),
//      nullable = rs.getInt("NULLABLE"),
//      size = rs.getInt("COLUMN_SIZE")
//    )
//}
//
