package io.exoquery

import io.exoquery.lang.*
import io.exoquery.util.TraceConfig
import io.exoquery.util.Tracer
import io.exoquery.util.stmt
import io.exoquery.util.unaryPlus
import io.exoquery.xr.XR

open class SqliteDialect(override val traceConf: TraceConfig = TraceConfig.Companion.empty) : SqlIdiom, BooleanLiteralSupport {
  override val concatFunction: String = "||"
  override val useActionTableAliasAs = SqlIdiom.ActionTableAliasBehavior.UseAs
  override val trace: Tracer by lazy { Tracer(traceType, traceConf, 1) }
  override val reservedKeywords: Set<String> = setOf(
    "abort",
    "action",
    "add",
    "after",
    "all",
    "alter",
    "analyze",
    "and",
    "as",
    "asc",
    "attach",
    "autoincrement",
    "before",
    "begin",
    "between",
    "by",
    "cascade",
    "case",
    "cast",
    "check",
    "collate",
    "column",
    "commit",
    "conflict",
    "constraint",
    "create",
    "cross",
    "current_date",
    "current_time",
    "current_timestamp",
    "database",
    "default",
    "deferrable",
    "deferred",
    "delete",
    "desc",
    "detach",
    "distinct",
    "drop",
    "each",
    "else",
    "end",
    "escape",
    "except",
    "exclusive",
    "exists",
    "explain",
    "fail",
    "for",
    "foreign",
    "from",
    "full",
    "glob",
    "group",
    "having",
    "if",
    "ignore",
    "immediate",
    "in",
    "index",
    "indexed",
    "initially",
    "inner",
    "insert",
    "instead",
    "intersect",
    "into",
    "is",
    "isnull",
    "join",
    "key",
    "left",
    "like",
    "limit",
    "match",
    "natural",
    "no",
    "not",
    "notnull",
    "null",
    "of",
    "offset",
    "on",
    "or",
    "order",
    "outer",
    "plan",
    "pragma",
    "primary",
    "query",
    "raise",
    "recursive",
    "references",
    "regexp",
    "reindex",
    "release",
    "rename",
    "replace",
    "restrict",
    "right",
    "rollback",
    "row",
    "savepoint",
    "select",
    "set",
    "table",
    "temp",
    "temporary",
    "then",
    "to",
    "transaction",
    "trigger",
    "union",
    "unique",
    "update",
    "using",
    "vacuum",
    "values",
    "view",
    "virtual",
    "when",
    "where",
    "with",
    "without"
  )

  override fun xrOrderByCriteriaTokenImpl(orderByCriteriaImpl: XR.OrderField): Token = with(orderByCriteriaImpl) {
    when {
      orderingOpt == null -> stmt("${scopedTokenizer(field)}")
      orderingOpt is XR.Ordering.Asc -> stmt("${scopedTokenizer(field)} ASC")
      orderingOpt is XR.Ordering.Desc -> stmt("${scopedTokenizer(field)} DESC")
      orderingOpt is XR.Ordering.AscNullsFirst -> stmt("${scopedTokenizer(field)} ASC /* NULLS FIRST */")
      orderingOpt is XR.Ordering.DescNullsFirst -> stmt("${scopedTokenizer(field)} DESC /* NULLS FIRST */")
      orderingOpt is XR.Ordering.AscNullsLast -> stmt("${scopedTokenizer(field)} ASC /* NULLS LAST */")
      orderingOpt is XR.Ordering.DescNullsLast -> stmt("${scopedTokenizer(field)} DESC /* NULLS LAST */")
      else -> xrError("Unsupported ordering: $orderingOpt")
    }
  }

  // Sqlite doesn't like parans around union clauses
  override fun xrSqlQueryModelTokenImpl(queryImpl: SqlQueryModel): Token = with(queryImpl) {
    when (this) {
      is FlattenSqlQuery -> token
      is SetOperationSqlQuery -> stmt("${a.token} ${op.token} ${b.token}")
      is UnaryOperationSqlQuery -> stmt("SELECT ${op.token} (${query.token})")
      is TopLevelFree -> this.value.token
    }
  }

  /**
   * Postgres OFFSET needs to be preceded by LIMIT
   * See here: https://sqlite.org/lang_select.html#simple_select_processing
   */
  override fun limitOffsetToken(query: Statement, limit: XR.Expression?, offset: XR.Expression?): Token =
    when {
      limit == null && offset == null -> query
      limit != null && offset == null -> stmt("$query LIMIT ${limit.token}")
      limit != null && offset != null -> stmt("$query LIMIT ${limit.token} OFFSET ${offset.token}")
      limit == null && offset != null -> stmt("$query LIMIT -1 OFFSET ${offset.token}")
      else -> throw IllegalStateException("Invalid limit/offset combination")
    }

  override fun stringConversionMapping(call: XR.MethodCall): Token = run {
    val name = call.name
    val head = call.head
    when (name) {
      "toLong" -> stmt("CAST(${head.token} AS BIGINT)")
      "toInt" -> stmt("CAST(${head.token} AS INTEGER)")
      "toShort" -> stmt("CAST(${head.token} AS SMALLINT)")
      "toDouble" -> stmt("CAST(${head.token} AS DOUBLE PRECISION)")
      "toFloat" -> stmt("CAST(${head.token} AS REAL)")
      "toBoolean" -> stmt("CASE WHEN ${head.token} = 'true' THEN 1 ELSE 0 END")
      "toString" -> stmt("${head.token}")
      else -> throw IllegalArgumentException("Unknown conversion function: ${name}")
    }
  }

  override fun jsonExtract(jsonExpr: XR.U.QueryOrExpression, pathExpr: XR.U.QueryOrExpression): Token =
    +"${jsonExpr.token} -> ${pathExpr.token}"

  override fun jsonExtractAsString(jsonExpr: XR.U.QueryOrExpression, pathExpr: XR.U.QueryOrExpression): Token =
    +"${jsonExpr.token} ->> ${pathExpr.token}"
}
