package io.exoquery.codegen.model

import java.math.BigDecimal
import kotlin.reflect.KClass
import java.sql.Types.*

// TODO move to using the Java enum JDBCType
class DefaultJdbcTyper(
  private val numericPreference: NumericPreference
) : (JdbcTypeInfo) -> KClass<*>? {

  private val maxIntDigits = 9
  private val maxLongDigits = 18

  override fun invoke(jdbcTypeInfo: JdbcTypeInfo): KClass<*>? {
    val jdbcType = jdbcTypeInfo.jdbcType
    val jdbcTypeName = jdbcTypeInfo.typeName?.lowercase()
    return when {
      jdbcType in setOf(CHAR, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR) -> String::class

      jdbcType == NUMERIC -> when {
        numericPreference == NumericPreference.PreferPrimitivesWhenPossible && jdbcTypeInfo.size <= maxIntDigits -> Int::class
        numericPreference == NumericPreference.PreferPrimitivesWhenPossible && jdbcTypeInfo.size <= maxLongDigits -> Long::class
        else -> BigDecimal::class
      }
      jdbcType == DECIMAL -> when {
        numericPreference == NumericPreference.PreferPrimitivesWhenPossible && jdbcTypeInfo.size <= maxIntDigits -> Int::class
        numericPreference == NumericPreference.PreferPrimitivesWhenPossible && jdbcTypeInfo.size <= maxLongDigits -> Long::class
        else -> BigDecimal::class
      }

      jdbcType in setOf(BIT, BOOLEAN) -> Boolean::class
      jdbcType == TINYINT -> Byte::class
      jdbcType == SMALLINT -> Short::class
      jdbcType == INTEGER -> Int::class
      jdbcType == BIGINT -> Long::class
      jdbcType == REAL -> Float::class
      jdbcType == FLOAT || jdbcType == DOUBLE -> Double::class
      jdbcType == DATE -> kotlinx.datetime.LocalDate::class
      jdbcType == TIME || jdbcType == TIMESTAMP -> kotlinx.datetime.LocalDateTime::class
      jdbcType == ARRAY -> null // arrays not supported yet

      jdbcType in setOf(BINARY, VARBINARY, LONGVARBINARY, BLOB) -> null
      jdbcType == STRUCT -> null
      jdbcType == REF -> null
      jdbcType == DATALINK -> null
      jdbcType == ROWID -> null
      jdbcType == NCLOB -> null
      jdbcType == SQLXML -> null
      jdbcType == NULL -> null

      jdbcType == CLOB -> null

      jdbcTypeName == "json" || jdbcTypeName == "jsonb" -> String::class
      jdbcTypeName == "uuid" -> String::class // TODO should depend on DatabaseType

      else -> null
    }
  }
}
