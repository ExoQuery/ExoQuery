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
    return when (jdbcType) {
      CHAR, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR -> String::class

      NUMERIC -> when {
        numericPreference == NumericPreference.PreferPrimitivesWhenPossible && jdbcTypeInfo.size <= maxIntDigits -> Int::class
        numericPreference == NumericPreference.PreferPrimitivesWhenPossible && jdbcTypeInfo.size <= maxLongDigits -> Long::class
        else -> BigDecimal::class
      }
      DECIMAL -> when {
        numericPreference == NumericPreference.PreferPrimitivesWhenPossible && jdbcTypeInfo.size <= maxIntDigits -> Int::class
        numericPreference == NumericPreference.PreferPrimitivesWhenPossible && jdbcTypeInfo.size <= maxLongDigits -> Long::class
        else -> BigDecimal::class
      }

      BIT, BOOLEAN -> Boolean::class
      TINYINT -> Byte::class
      SMALLINT -> Short::class
      INTEGER -> Int::class
      BIGINT -> Long::class
      REAL -> Float::class
      FLOAT, DOUBLE -> Double::class
      DATE -> kotlinx.datetime.LocalDate::class
      TIME, TIMESTAMP -> kotlinx.datetime.LocalDateTime::class
      ARRAY -> null // arrays not supported yet

      BINARY, VARBINARY, LONGVARBINARY, BLOB -> null
      STRUCT -> null
      REF -> null
      DATALINK -> null
      ROWID -> null
      NCLOB -> null
      SQLXML -> null
      NULL -> null

      CLOB -> null
      else -> null
    }
  }
}
