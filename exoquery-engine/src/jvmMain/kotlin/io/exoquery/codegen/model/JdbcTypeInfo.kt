package io.exoquery.codegen.model

data class JdbcTypeInfo(val jdbcType: Int, val size: Int, val typeName: String?) {
  companion object {
    fun fromColumnMeta(columnMeta: ColumnMeta): JdbcTypeInfo {
      return JdbcTypeInfo(columnMeta.dataType, columnMeta.size, columnMeta.typeName)
    }
  }
}

// Scala:
//case class JdbcTypeInfo(jdbcType: Int, size: Int, typeName: Option[String])
//object JdbcTypeInfo {
//  def apply(cs: JdbcColumnMeta): JdbcTypeInfo = JdbcTypeInfo(cs.dataType, cs.size, Some(cs.typeName))
//}
