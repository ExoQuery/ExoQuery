package io.exoquery.lang

import io.exoquery.xr.XRType

abstract class StatelessQueryTransformer {
  open operator fun invoke(q: SqlQueryModel, topLevelQuat: XRType = XRType.Unknown): SqlQueryModel =
    invoke(q, QueryLevel.Top(topLevelQuat))

  protected open fun invoke(q: SqlQueryModel, level: QueryLevel): SqlQueryModel =
    with(q) {
      when (this) {
        is FlattenSqlQuery -> expandNested(this, level)
        is SetOperationSqlQuery -> SetOperationSqlQuery(invoke(a, level), op, invoke(b, level), type)
        is UnaryOperationSqlQuery -> UnaryOperationSqlQuery(op, invoke(query, level), type)
        is TopLevelFree -> this
      }
    }

  protected abstract fun expandNested(q: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery


  protected open fun expandContext(s: FromContext): FromContext =
    with(s) {
      when (this) {
        is QueryContext -> QueryContext(invoke(query, QueryLevel.Inner), alias)
        is FlatJoinContext -> FlatJoinContext(joinType, expandContext(from), on)
        is TableContext, is ExpressionContext -> this
      }
    }
}
