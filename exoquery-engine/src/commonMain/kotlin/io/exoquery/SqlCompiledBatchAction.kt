package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.lang.ParamBatchTokenRealized
import io.exoquery.lang.StatelessTokenTransformer
import io.exoquery.lang.Token
import io.exoquery.printing.PrintMisc
import io.exoquery.xr.XR

data class SqlCompiledBatchAction<BatchInput, Input : Any, Output> @ExoInternal constructor(
  override val value: String,
  override val token: Token,
  val needsTokenization: Boolean,
  val actionKind: ActionKind,
  val actionReturningKind: ActionReturningKind,
  val batchParam: Sequence<BatchInput>,
  val label: String?,
  val debugData: DebugData
) : ExoCompiled() {
  @ExoInternal
  override val params: List<Param<*>> by lazy { token.extractParams() }
  @ExoInternal
  override fun originalXR(): XR = debugData.originalXR()

  @ExoInternal
  val effectiveQuery by lazy { token.build() }

  protected fun paramsForElement(elem: BatchInput) =
    params.map { param ->
      when (param) {
        is ParamBatchRefiner<*, *> -> param.refineAny(elem as Any?)
        else -> param
      }
    }


  private fun realizeToken(token: Token, value: BatchInput) =
    StatelessTokenTransformer.Companion.invoke {
      when (it) {
        is ParamBatchTokenRealized -> {
          it.refineAny(value)
        }
        else -> null
      }
    }.invoke(token)

  @ExoInternal
  fun produceBatchGroups(): Sequence<BatchParamGroup<BatchInput, Input, Output>> =
    batchParam.map { BatchParamGroup(it, paramsForElement(it), { realizeToken(token, it) }, debugData) }

  @ExoInternal
  override fun determinizeDynamics(): SqlCompiledBatchAction<BatchInput, Input, Output> =
    this.copy(token = determinizedToken())

  @ExoInternal
  fun withNonStrictEquality() = this.copy(token = token.withNonStrictEquality())

  fun show() = PrintMisc().invoke(this)

  @ExoInternal
  data class DebugData(val phase: Phase, val originalXR: () -> XR.Batching)
}

// Effective token refines tokens for that particular batch-input value, we only need it for debugging
@ExoInternal
data class BatchParamGroup<BatchInput, Input : Any, Output>(val input: BatchInput, val params: List<Param<*>>, val effectiveToken: () -> Token, val debugData: SqlCompiledBatchAction.DebugData) {
  @ExoInternal
  fun determinizeDynamics(): BatchParamGroup<BatchInput, Input, Output> = run {
    val (token, params) = determinizeToken(effectiveToken(), params)
    this.copy(params = params, effectiveToken = { token })
  }
  @ExoInternal
  fun originalXR(): XR = debugData.originalXR()
}
