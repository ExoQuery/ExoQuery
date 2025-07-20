package io.exoquery.codegen.model

import java.math.BigDecimal
import kotlin.reflect.KClass
import java.sql.Types.*

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

      DECIMAL -> BigDecimal::class
      BIT, BOOLEAN -> Boolean::class
      TINYINT -> Byte::class
      SMALLINT -> Short::class
      INTEGER -> Int::class
      BIGINT -> Long::class
      REAL -> Float::class
      FLOAT, DOUBLE -> Double::class
      DATE -> java.time.LocalDate::class
      TIME, TIMESTAMP -> java.time.LocalDateTime::class
      ARRAY -> java.sql.Array::class

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

// Scala:
//class DefaultJdbcTyper(
//  strategy: UnrecognizedTypeStrategy,
//  numericPreference: NumericPreference
//) extends (JdbcTypeInfo => Option[ClassTag[_]]) {
//
//  private[getquill] val MaxIntDigits  = 9
//  private[getquill] val MaxLongDigits = 18
//
//  def unresolvedType(jdbcType: Int, tag: ClassTag[_]): Option[ClassTag[_]] =
//    unresolvedType(jdbcType, Some(tag))
//
//  def unresolvedType(jdbcType: Int, tag: Option[ClassTag[_]]): Option[ClassTag[_]] =
//    strategy match {
//      case AssumeString => Some(classTag[String])
//      case SkipColumn   => None
//      case ThrowTypingError =>
//        throw new TypingError(
//          s"Could not resolve jdbc type: ${jdbcType}" + tag.map(t => s" class: `${t}`.").getOrElse("")
//        )
//    }
//
//  def apply(jdbcTypeInfo: JdbcTypeInfo): Option[ClassTag[_]] = {
//
//    val jdbcType = jdbcTypeInfo.jdbcType
//
//    jdbcType match {
//      case CHAR | VARCHAR | LONGVARCHAR | NCHAR | NVARCHAR | LONGNVARCHAR => Some(classTag[String])
//      case NUMERIC =>
//        numericPreference match {
//          case PreferPrimitivesWhenPossible if (jdbcTypeInfo.size <= MaxIntDigits)  => Some(classTag[Int])
//          case PreferPrimitivesWhenPossible if (jdbcTypeInfo.size <= MaxLongDigits) => Some(classTag[Long])
//          case _                                                                    => Some(classTag[BigDecimal])
//        }
//      case DECIMAL        => Some(classTag[BigDecimal])
//      case BIT | BOOLEAN  => Some(classTag[Boolean])
//      case TINYINT        => Some(classTag[Byte])
//      case SMALLINT       => Some(classTag[Short])
//      case INTEGER        => Some(classTag[Int])
//      case BIGINT         => Some(classTag[Long])
//      case REAL           => Some(classTag[Float])
//      case FLOAT | DOUBLE => Some(classTag[Double])
//      case DATE           => Some(classTag[java.time.LocalDate])
//      case TIME           => Some(classTag[java.time.LocalDateTime])
//      case TIMESTAMP      => Some(classTag[java.time.LocalDateTime])
//      case ARRAY          => Some(classTag[java.sql.Array])
//
//      case BINARY | VARBINARY | LONGVARBINARY | BLOB => unresolvedType(jdbcType, classTag[java.sql.Blob])
//      case STRUCT                                    => unresolvedType(jdbcType, classTag[java.sql.Struct])
//      case REF                                       => unresolvedType(jdbcType, classTag[java.sql.Ref])
//      case DATALINK                                  => unresolvedType(jdbcType, classTag[java.net.URL])
//      case ROWID                                     => unresolvedType(jdbcType, classTag[java.sql.RowId])
//      case NCLOB                                     => unresolvedType(jdbcType, classTag[java.sql.NClob])
//      case SQLXML                                    => unresolvedType(jdbcType, classTag[java.sql.SQLXML])
//      case NULL                                      => unresolvedType(jdbcType, classTag[Null])
//
//      case CLOB =>
//        unresolvedType(jdbcType, classTag[java.sql.Clob])
//
//      case other =>
//        unresolvedType(other, None)
//    }
//  }
//}
