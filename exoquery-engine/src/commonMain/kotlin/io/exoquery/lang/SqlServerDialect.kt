package io.exoquery

import io.exoquery.lang.*
import io.exoquery.ActionKind
import io.exoquery.util.TraceConfig
import io.exoquery.util.Tracer
import io.exoquery.xr.XR
import io.exoquery.util.unaryPlus
import io.exoquery.xr.BetaReduction
import io.exoquery.xr.BinaryOperator
import io.exoquery.xr.OP
import io.exoquery.xr.toActionKind

open class SqlServerDialect(override val traceConf: TraceConfig = TraceConfig.empty) : SqlIdiom, BooleanLiteralSupport {
  override val concatFunction: String = "+"
  override val useActionTableAliasAs = SqlIdiom.ActionTableAliasBehavior.UseAs
  override val trace: Tracer by lazy { Tracer(traceType, traceConf, 1) }
  override val reservedKeywords: Set<String> = setOf(
    "add",
    "all",
    "alter",
    "and",
    "any",
    "as",
    "asc",
    "authorization",
    "backup",
    "begin",
    "between",
    "break",
    "browse",
    "bulk",
    "by",
    "cascade",
    "case",
    "check",
    "checkpoint",
    "close",
    "clustered",
    "coalesce",
    "collate",
    "column",
    "commit",
    "compute",
    "constraint",
    "contains",
    "containstable",
    "continue",
    "convert",
    "create",
    "cross",
    "current",
    "current_date",
    "current_time",
    "current_timestamp",
    "current_user",
    "cursor",
    "database",
    "dbcc",
    "deallocate",
    "declare",
    "default",
    "delete",
    "deny",
    "desc",
    "disk",
    "distinct",
    "distributed",
    "double",
    "drop",
    "dump",
    "else",
    "end",
    "errlvl",
    "escape",
    "except",
    "exec",
    "execute",
    "exists",
    "exit",
    "external",
    "fetch",
    "file",
    "fillfactor",
    "for",
    "foreign",
    "freetext",
    "freetexttable",
    "from",
    "full",
    "function",
    "goto",
    "grant",
    "group",
    "having",
    "holdlock",
    "identity",
    "identity_insert",
    "identitycol",
    "if",
    "in",
    "index",
    "inner",
    "insert",
    "intersect",
    "into",
    "is",
    "join",
    "key",
    "kill",
    "left",
    "like",
    "lineno",
    "load",
    "merge",
    "national",
    "nocheck",
    "nonclustered",
    "not",
    "null",
    "nullif",
    "of",
    "off",
    "offsets",
    "on",
    "open",
    "opendatasource",
    "openquery",
    "openrowset",
    "openxml",
    "option",
    "or",
    "order",
    "outer",
    "over",
    "percent",
    "pivot",
    "plan",
    "precision",
    "primary",
    "print",
    "proc",
    "procedure",
    "public",
    "raiserror",
    "read",
    "readtext",
    "reconfigure",
    "references",
    "replication",
    "restore",
    "restrict",
    "return",
    "revert",
    "revoke",
    "right",
    "rollback",
    "rowcount",
    "rowguidcol",
    "rule",
    "save",
    "schema",
    "securityaudit",
    "select",
    "semantickeyphrasetable",
    "semanticsimilaritydetailstable",
    "semanticsimilaritytable",
    "session_user",
    "set",
    "setuser",
    "shutdown",
    "some",
    "statistics",
    "system_user",
    "table",
    "tablesample",
    "textsize",
    "then",
    "to",
    "top",
    "tran",
    "transaction",
    "trigger",
    "truncate",
    "try_convert",
    "tsequal",
    "union",
    "unique",
    "unpivot",
    "update",
    "updatetext",
    "use",
    "user",
    "values",
    "varying",
    "view",
    "waitfor",
    "when",
    "where",
    "while",
    "with",
    "within group",
    "writetext"
  )

//  override protected def limitOffsetToken(
//    query: Statement
//  )(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[(Option[Ast], Option[Ast])] =
//    Tokenizer[(Option[Ast], Option[Ast])] {
//      case (Some(limit), None)         => stmt"TOP (${limit.token}) $query"
//      case (Some(limit), Some(offset)) => stmt"$query OFFSET ${offset.token} ROWS FETCH FIRST ${limit.token} ROWS ONLY"
//      case (None, Some(offset))        => stmt"$query OFFSET ${offset.token} ROWS"
//      case other                       => super.limitOffsetToken(query).token(other)
//    }

  override fun limitOffsetToken(query: Statement, limit: XR.Expression?, offset: XR.Expression?): Token =
    when {
      limit != null && offset == null -> +"TOP (${limit.token}) $query"
      limit != null && offset != null -> +"$query OFFSET ${offset.token} ROWS FETCH FIRST ${limit.token} ROWS ONLY"
      limit == null && offset != null -> +"$query OFFSET ${offset.token} ROWS"
      else -> query
    }


  override fun xrReturningTokenImpl(query: XR.Returning): Token =
    when {
      query.kind is XR.Returning.Kind.Expression && query.action.toActionKind() == ActionKind.Insert && query.action is XR.Insert ->
        tokenizeOutputtingInsert(query.kind, query.action)

      query.kind is XR.Returning.Kind.Expression && query.action.toActionKind() == ActionKind.Update && query.action is XR.Update ->
        tokenizeOutputtingUpdate(query.kind, query.action)
      query.kind is XR.Returning.Kind.Expression && query.action.toActionKind() == ActionKind.Update && query.action is XR.FilteredAction && query.action.action is XR.Update ->
        tokenizeOutputtingUpdate(query.kind, query.action.action, query.action)

      query.kind is XR.Returning.Kind.Expression && query.action.toActionKind() == ActionKind.Delete && query.action is XR.Delete ->
        tokenizeOutputtingDelete(query.kind, query.action)
      query.kind is XR.Returning.Kind.Expression && query.action.toActionKind() == ActionKind.Delete && query.action is XR.FilteredAction && query.action.action is XR.Delete ->
        tokenizeOutputtingDelete(query.kind, query.action.action, query.action)

      // If the returning-kind is Keys then we don't use the output-clause and rely on Sttement.generatedKeys (and similar APIs)
      else ->
        super<SqlIdiom>.xrReturningTokenImpl(query)
    }

  fun tokenizeOutputtingInsert(expr: XR.Returning.Kind.Expression, insert: XR.Insert): Token = run {
    val newAlias = insert.coreAlias().copy(name = "INSERTED")
    val returningClauseToken = protractReturning(expr, newAlias)
    val query = insert.query
    with(insert) {
      val (columns, values) = columnsAndValues(assignments, exclusions).unzip()
      +"INSERT INTO ${query.token}${`AS_table`(alias)} (${columns.mkStmt(", ")}) OUTPUT ${returningClauseToken} VALUES ${tokenizeInsertAssignemnts(values)}"
    }
  }

  fun tokenizeOutputtingUpdate(expr: XR.Returning.Kind.Expression, update: XR.Update, filterRaw: XR.FilteredAction? = null): Token = run {
    val newAlias = update.coreAlias().copy(name = "INSERTED")
    val returningClauseToken = protractReturning(expr, newAlias)
    fun updateBase() = +"${tokenizeUpdateBase(update)} OUTPUT ${returningClauseToken}"
    when {
      filterRaw != null -> {
        val filterWithCorrectAlias = BetaReduction(filterRaw.filter, filterRaw.alias to update.alias).asExpr()
        +"${updateBase()} WHERE ${filterWithCorrectAlias.token}"
      }
      else ->
        updateBase()
    }
  }

  fun tokenizeOutputtingDelete(expr: XR.Returning.Kind.Expression, delete: XR.Delete, filterRaw: XR.FilteredAction? = null): Token = run {
    val newAlias = delete.coreAlias().copy(name = "DELETED")
    val returningClauseToken = protractReturning(expr, newAlias)
    fun deleteBase() = +"${tokenizeDeleteBase(delete)} OUTPUT ${returningClauseToken}"
    when {
      filterRaw != null -> {
        val filterWithCorrectAlias = BetaReduction(filterRaw.filter, filterRaw.alias to delete.alias).asExpr()
        +"${deleteBase()} WHERE ${filterWithCorrectAlias.token}"
      }
      else ->
        deleteBase()
    }
  }

  override fun opBinaryTokenImpl(opImpl: BinaryOperator): Token = with(opImpl) {
    when (this) {
      is OP.StrPlus -> +"+"
      else -> super<SqlIdiom>.opBinaryTokenImpl(opImpl)
    }
  }

  override fun stringConversionMapping(call: XR.MethodCall): Token = run {
    val name = call.name
    val head = call.head
    when (name) {
      "toLong" -> +"CAST(${head.token} AS BIGINT)"
      "toInt" -> +"CAST(${head.token} AS INTEGER)"
      "toShort" -> +"CAST(${head.token} AS SMALLINT)"
      "toDouble" -> +"CAST(${head.token} AS DOUBLE PRECISION)"
      "toFloat" -> +"CAST(${head.token} AS REAL)"
      "toBoolean" -> +"CASE WHEN ${head.token} = 'true' THEN 1 ELSE 0 END"
      "toString" -> +"${head.token}"
      else -> throw IllegalArgumentException("Unknown conversion function: ${name}")
    }
  }

  private fun addPrefix(pathExpr: XR.U.QueryOrExpression) =
    when (pathExpr) {
      is XR.Const.String ->
        XR.Const.String("$.${pathExpr.value}")
      else ->
        pathExpr
    }

  override fun jsonExtract(jsonExpr: XR.U.QueryOrExpression, pathExpr: XR.U.QueryOrExpression): Token =
    +"JSON_QUERY(${jsonExpr.token}, ${addPrefix(pathExpr).token})"

  override fun jsonExtractAsString(jsonExpr: XR.U.QueryOrExpression, pathExpr: XR.U.QueryOrExpression): Token =
    +"JSON_VALUE(${jsonExpr.token}, ${addPrefix(pathExpr).token})"
}
