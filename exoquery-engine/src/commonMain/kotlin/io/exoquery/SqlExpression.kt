package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.printing.PrintMisc
import io.exoquery.xr.XR
import io.exoquery.xr.swapTags

sealed interface ContainerOfXR {
  val xr: XR

  // I.e. runtime containers that are used in the expression (if any)
  val runtimes: RuntimeSet
  val params: ParamSet

  fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): ContainerOfXR
  fun withNonStrictEquality(): ContainerOfXR
}


// Less fungible i.e. always top-level and only for actions
sealed interface ContainerOfActionXR : ContainerOfXR {
  override val xr: XR.Action
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): ContainerOfActionXR
  override fun withNonStrictEquality(): ContainerOfActionXR
}

// Specifically for fungible XR Query and Expression types that are composable (e.g. that can be in a RuntimeSet)
sealed interface ContainerOfFunXR : ContainerOfXR {
  override val xr: XR.U.QueryOrExpression
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): ContainerOfFunXR
  override fun withNonStrictEquality(): ContainerOfFunXR
}

data class SqlExpression<T>(override val xr: XR.Expression, override val runtimes: RuntimeSet, override val params: ParamSet) : ContainerOfFunXR {
  @ExoInternal
  fun determinizeDynamics(): SqlExpression<T> = DeterminizeDynamics().ofExpression(this)

  fun show() = PrintMisc().invoke(this)

  @ExoInternal
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): SqlExpression<T> =
    copy(xr = xr as? XR.Expression ?: xrError("Failed to rebuild SqlExpression with XR of type ${xr::class} which was: ${xr.show()}"), runtimes = runtimes, params = params)

  @ExoInternal
  override fun withNonStrictEquality(): SqlExpression<T> =
    copy(params = params.withNonStrictEquality())
}

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
    return SqlExpression(newXR, RuntimeSet(newRuntimes.map { it.second to it.third }), ParamSet(newParams.map { it.third }))
  }

  private fun <T> recQuery(query: SqlQuery<T>): SqlQuery<T> {
    val newParams = query.walkParams()
    val newRuntimes = query.walkRuntimes()
    val newXR = query.xr.swapTags(
      (newParams.map { it.first to it.second } + newRuntimes.map { it.first to it.second }).toMap()
    )
    return SqlQuery(newXR, RuntimeSet(newRuntimes.map { it.second to it.third }), ParamSet(newParams.map { it.third }))
  }

  private fun <Input, Output> recAction(action: SqlAction<Input, Output>): SqlAction<Input, Output> {
    val newParams = action.walkParams()
    val newRuntimes = action.walkRuntimes()
    val newXR = action.xr.swapTags(
      (newParams.map { it.first to it.second } + newRuntimes.map { it.first to it.second }).toMap()
    )
    return SqlAction(newXR, RuntimeSet(newRuntimes.map { it.second to it.third }), ParamSet(newParams.map { it.third }))
  }

  private fun <BatchInput, Input : Any, Output> recBatching(action: SqlBatchAction<BatchInput, Input, Output>): SqlBatchAction<BatchInput, Input, Output> {
    val newParams = action.walkParams()
    val newRuntimes = action.walkRuntimes()
    val newXR = action.xr.swapTags(
      (newParams.map { it.first to it.second } + newRuntimes.map { it.first to it.second }).toMap()
    )
    return SqlBatchAction(newXR, action.batchParam, RuntimeSet(newRuntimes.map { it.second to it.third }), ParamSet(newParams.map { it.third }))
  }

  fun <BI, I : Any, O> ofBatchAction(action: SqlBatchAction<BI, I, O>): SqlBatchAction<BI, I, O> = recBatching(action)
  fun <I, O> ofAction(action: SqlAction<I, O>): SqlAction<I, O> = recAction(action)
  fun <T> ofExpression(expr: SqlExpression<T>): SqlExpression<T> = recExpr(expr)
  fun <T> ofQuery(query: SqlQuery<T>): SqlQuery<T> = recQuery(query)
}
