package io.exoquery

import io.exoquery.lang.*
import io.exoquery.util.TraceConfig
import io.exoquery.util.Tracer
import io.exoquery.util.unaryPlus
import io.exoquery.xr.BetaReduction
import io.exoquery.xr.BetaReduction.Companion.invoke
import io.exoquery.xr.XR

open class PostgresDialect(override val traceConf: TraceConfig = TraceConfig.Companion.empty) : SqlIdiom {
  override val useActionTableAliasAs = SqlIdiom.ActionTableAliasBehavior.UseAs
  override val reservedKeywords: Set<String> = setOf(
    "all",
    "analyse",
    "analyze",
    "and",
    "any",
    "array",
    "as",
    "asc",
    "asymmetric",
    "authorization",
    "binary",
    "both",
    "case",
    "cast",
    "check",
    "collate",
    "column",
    "constraint",
    "create",
    "cross",
    "current_catalog",
    "current_date",
    "current_role",
    "current_schema",
    "current_time",
    "current_timestamp",
    "current_user",
    "default",
    "deferrable",
    "desc",
    "distinct",
    "do",
    "else",
    "end",
    "except",
    "false",
    "fetch",
    "for",
    "foreign",
    "from",
    "grant",
    "group",
    "having",
    "in",
    "initially",
    "intersect",
    "into",
    "lateral",
    "leading",
    "limit",
    "localtime",
    "localtimestamp",
    "not",
    "null",
    "offset",
    "on",
    "only",
    "or",
    "order",
    "placing",
    "primary",
    "references",
    "returning",
    "select",
    "session_user",
    "some",
    "symmetric",
    "table",
    "then",
    "to",
    "trailing",
    "true",
    "union",
    "unique",
    "user",
    "using",
    "variadic",
    "when",
    "where",
    "window",
    "with"
  )

  override val trace: Tracer by lazy { Tracer(traceType, traceConf, 1) }

  /**
   * For Postgres, quotation is only really needed if a quoted table or column has upper-case characters
   * since everything is lower-cased when quotation is not done. In that case we don't need to quote.
   */
  override fun tokenizeTable(name: String, hasRename: XR.HasRename): Token = escapeIfNeeded(name, hasRename.hasOrNot() && name.any { it.isUpperCase() })
  override fun tokenizeColumn(name: String, hasRename: XR.HasRename): Token = escapeIfNeeded(name, hasRename.hasOrNot() && name.any { it.isUpperCase() })

  override fun jsonExtract(jsonExpr: XR.U.QueryOrExpression, pathExpr: XR.U.QueryOrExpression): Token =
    +"${jsonExpr.token} -> ${pathExpr.token}"

  override fun jsonExtractAsString(jsonExpr: XR.U.QueryOrExpression, pathExpr: XR.U.QueryOrExpression): Token =
    +"${jsonExpr.token} ->> ${pathExpr.token}"

  override fun stringConversionMapping(call: XR.MethodCall): Token = run {
    fun scopedTokenizer(expr: XR.U.QueryOrExpression): Token =
      when (expr) {
        is XR.QueryToExpr -> scopedTokenizer(expr.head)
        is XR.ExprToQuery -> scopedTokenizer(expr.head)
        is XR.GlobalCall if expr.name == XR.FqName.JsonExtractAsString -> +"(${expr.token})"
        else -> expr.token
      }
    val name = call.name
    val head = call.head
    when (name) {
      "toLong" -> +"${scopedTokenizer(head).token}::BIGINT"
      "toInt" -> +"${scopedTokenizer(head).token}::INTEGER"
      "toShort" -> +"${scopedTokenizer(head).token}::SMALLINT"
      "toDouble" -> +"${scopedTokenizer(head).token}::DOUBLE PRECISION"
      "toFloat" -> +"${scopedTokenizer(head).token}::REAL"
      "toBoolean" -> +"${scopedTokenizer(head).token}::BOOLEAN"
      "toBigDecimal" -> +"${scopedTokenizer(head).token}::DECIMAL"
      "toString" -> +"${head.token}"
      else -> throw IllegalArgumentException("Unknown conversion function: ${name}")
    }
  }
  override fun wholeNumberConversionMapping(head: XR.U.QueryOrExpression, name: String, isKotlinSynthetic: Boolean): Token = run {
    when {
      // Do numeric casts to decimal types, but only if the user explicitly does it
      // we do not want to do it implicitly because Kotlin frequently does this and most DBs
      // do implicit float<->int conversions just as well
      name == "toDouble" && !isKotlinSynthetic -> +"${scopedTokenizer(head).token}:DOUBLE PRECISION"
      name == "toFloat" && !isKotlinSynthetic -> +"${scopedTokenizer(head).token}:REAL"
      name == "toBoolean" -> +"${scopedTokenizer(head).token}::BOOLEAN"
      name == "toString" -> +"${scopedTokenizer(head).token}::${varcharType()}"
      name == "toBigDecimal" -> +"${scopedTokenizer(head).token}::DECIMAL"
      // toInt, toLong, toShort reply in implicit casting
      else -> +"${head.token}"
    }
  }
  override fun floatConversionMapping(head: XR.U.QueryOrExpression, name: String, isKotlinSynthetic: Boolean): Token = run {
    when {
      name == "toLong" && !isKotlinSynthetic -> +"${scopedTokenizer(head).token}::BIGINT"
      name == "toInt" && !isKotlinSynthetic -> +"${scopedTokenizer(head).token}::INTEGER"
      name == "toShort" && !isKotlinSynthetic -> +"${scopedTokenizer(head).token}::SMALLINT"
      name == "toBoolean" -> +"${scopedTokenizer(head).token}::BOOLEAN"
      name == "toString" -> +"${scopedTokenizer(head).token}::${varcharType()}"
      // toFloat, toDouble, toBigDecimal reply in implicit casting
      else -> +"${head.token}"
    }
  }
}
