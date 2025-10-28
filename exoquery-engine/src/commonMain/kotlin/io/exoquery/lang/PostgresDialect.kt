package io.exoquery

import io.exoquery.lang.*
import io.exoquery.util.TraceConfig
import io.exoquery.util.Tracer
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
}
