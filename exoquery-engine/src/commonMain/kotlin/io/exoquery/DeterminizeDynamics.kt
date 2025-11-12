package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.xr.swapTags

@ExoInternal
internal class DeterminizeDynamics() {
  private var id = 0
  private fun nextId() = "$id".also { id++ }

  private fun recContainer(expr: ContainerOfXR): ContainerOfXR =
    when (expr) {
      is SqlExpression<*> -> recExpr(expr)
      is SqlQuery<*> -> recQuery(expr)
      is SqlAction<*, *> -> recAction(expr)
      else -> error("Unknown container type: ${expr::class} of ${expr}")
    }

  private fun ContainerOfXR.walkParams() =
    params.lifts.map { param ->
      val newBid = BID(nextId())
      val newParam = when (param) {
        is ParamSingle<*> -> param.copy(id = newBid)
        is ParamMulti<*> -> param.copy(id = newBid)
        is ParamBatchRefiner<*, *> -> param.copy(id = newBid)
      }
      Triple(param.id, newBid, newParam)
    }

  private fun ContainerOfXR.walkRuntimes() =
    runtimes.runtimes.map { (bid, container) ->
      val newContainer = recContainer(container)
      val newBid = BID(nextId())
      Triple(bid, newBid, newContainer)
    }

  private fun <T> recExpr(expr: SqlExpression<T>): SqlExpression<T> {
    val newParams = expr.walkParams()
    val newRuntimes = expr.walkRuntimes()
    val newXR = expr.xr.swapTags(
      (newParams.map { it.first to it.second } + newRuntimes.map { it.first to it.second }).toMap()
    )
    return SqlExpression({ newXR }, RuntimeSet(newRuntimes.map { it.second to it.third }), ParamSet(newParams.map { it.third }))
  }

  private fun <T> recQuery(query: SqlQuery<T>): SqlQuery<T> {
    val newParams = query.walkParams()
    val newRuntimes = query.walkRuntimes()
    val newXR = query.xr.swapTags(
      (newParams.map { it.first to it.second } + newRuntimes.map { it.first to it.second }).toMap()
    )
    return SqlQuery({ newXR }, RuntimeSet(newRuntimes.map { it.second to it.third }), ParamSet(newParams.map { it.third }))
  }

  private fun <Input, Output> recAction(action: SqlAction<Input, Output>): SqlAction<Input, Output> {
    val newParams = action.walkParams()
    val newRuntimes = action.walkRuntimes()
    val newXR = action.xr.swapTags(
      (newParams.map { it.first to it.second } + newRuntimes.map { it.first to it.second }).toMap()
    )
    return SqlAction({ newXR }, RuntimeSet(newRuntimes.map { it.second to it.third }), ParamSet(newParams.map { it.third }))
  }

  private fun <BatchInput, Input : Any, Output> recBatching(action: SqlBatchAction<BatchInput, Input, Output>): SqlBatchAction<BatchInput, Input, Output> {
    val newParams = action.walkParams()
    val newRuntimes = action.walkRuntimes()
    val newXR = action.xr.swapTags(
      (newParams.map { it.first to it.second } + newRuntimes.map { it.first to it.second }).toMap()
    )
    return SqlBatchAction({ newXR }, action.batchParam, RuntimeSet(newRuntimes.map { it.second to it.third }), ParamSet(newParams.map { it.third }))
  }

  fun <BI, I : Any, O> ofBatchAction(action: SqlBatchAction<BI, I, O>): SqlBatchAction<BI, I, O> = recBatching(action)
  fun <I, O> ofAction(action: SqlAction<I, O>): SqlAction<I, O> = recAction(action)
  fun <T> ofExpression(expr: SqlExpression<T>): SqlExpression<T> = recExpr(expr)
  fun <T> ofQuery(query: SqlQuery<T>): SqlQuery<T> = recQuery(query)
}
