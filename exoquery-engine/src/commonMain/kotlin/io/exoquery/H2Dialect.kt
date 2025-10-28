package io.exoquery

import io.exoquery.lang.SqlIdiom
import io.exoquery.util.TraceConfig
import io.exoquery.util.Tracer

/**
 * H2Dialect is a dialect for H2 database.
 *
 * TODO the only major difference with the standard SqlIdiom/Postgres is on-conflict rendering,
 *      need to add then when OnConflict is implemented.
 */
open class H2Dialect(override val traceConf: TraceConfig = TraceConfig.empty) : SqlIdiom {
  override val concatFunction: String = "||"
  override val useActionTableAliasAs = SqlIdiom.ActionTableAliasBehavior.UseAs
  override val reservedKeywords: Set<String> = setOf(
    "add",
    "all",
    "alter",
    "and",
    "any",
    "as",
    "asc",
    "between",
    "by",
    "case",
    "check",
    "column",
    "constraint",
    "create",
    "cross",
    "current_date",
    "current_time",
    "current_timestamp",
    "delete",
    "desc",
    "distinct",
    "drop",
    "else",
    "exists",
    "for",
    "foreign",
    "from",
    "full",
    "group",
    "having",
    "in",
    "inner",
    "insert",
    "intersect",
    "into",
    "is",
    "join",
    "left",
    "like",
    "limit",
    "minus",
    "natural",
    "not",
    "null",
    "on",
    "or",
    "order",
    "outer",
    "primary",
    "right",
    "second",
    "select",
    "set",
    "table",
    "then",
    "to",
    "union",
    "unique",
    "update",
    "using",
    "value",
    "values",
    "when",
    "where"
  )

  override val trace: Tracer by lazy { Tracer(traceType, traceConf, 1) }
}
