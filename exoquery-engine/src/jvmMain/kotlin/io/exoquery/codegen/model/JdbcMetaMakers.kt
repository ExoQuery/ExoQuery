package io.exoquery.codegen.model

import java.sql.ResultSet

object JdbcMetaMakers {
  fun makeTableMeta(rs: ResultSet) =
    TableMeta(
      tableCat = rs.getString("TABLE_CAT"),
      tableSchema = rs.getString("TABLE_SCHEM"),
      tableName = rs.getString("TABLE_NAME"),
      tableType = rs.getString("TABLE_TYPE")
    )

  fun makeColumnMeta(rs: ResultSet) =
    ColumnMeta(
      tableCat = rs.getString("TABLE_CAT"),
      tableSchema = rs.getString("TABLE_SCHEM"),
      tableName = rs.getString("TABLE_NAME"),
      columnName = rs.getString("COLUMN_NAME"),
      dataType = rs.getInt("DATA_TYPE"),
      typeName = rs.getString("TYPE_NAME"),
      nullable = rs.getInt("NULLABLE"),
      size = rs.getInt("COLUMN_SIZE")
    )
}
