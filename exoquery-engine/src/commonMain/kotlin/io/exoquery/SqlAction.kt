package io.exoquery

import io.exoquery.annotation.CapturedDynamic
import io.exoquery.annotation.ExoBuildFunctionLabel
import io.exoquery.annotation.ExoInternal
import io.exoquery.printing.PrintMisc
import io.exoquery.lang.SqlIdiom
import io.exoquery.xr.RuntimeBuilder
import io.exoquery.xr.XR
import io.exoquery.xr.toActionKind

data class SqlAction<Input, Output>(override val xr: XR.Action, override val runtimes: RuntimeSet, override val params: ParamSet) : ContainerOfXR {
  fun show() = PrintMisc().invoke(this)

  @ExoInternal
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): SqlAction<Input, Output> =
    copy(xr = xr as? XR.Action ?: xrError("Failed to rebuild SqlAction with XR of type ${xr::class} which was: ${xr.show()}"), runtimes = runtimes, params = params)

  @ExoInternal
  fun buildRuntime(dialect: SqlIdiom, label: String?, pretty: Boolean = false): SqlCompiledAction<Input, Output> = run {
    val containerBuild = RuntimeBuilder(dialect, pretty).forAction(this)
    val actionReturningKind = ActionReturningKind.fromActionXR(xr)
    SqlCompiledAction(
      containerBuild.queryString, containerBuild.queryTokenized, true, xr.toActionKind(), actionReturningKind, label,
      SqlCompiledAction.DebugData(Phase.Runtime, { xr })
    )
  }

  fun <Dialect : SqlIdiom> build(): SqlCompiledAction<Input, Output> = errorCap("The build function body was not inlined")
  fun <Dialect : SqlIdiom> build(@ExoBuildFunctionLabel label: String): SqlCompiledAction<Input, Output> = errorCap("The build function body was not inlined")

  fun <Dialect : SqlIdiom> buildPretty(): SqlCompiledAction<Input, Output> = errorCap("The buildPretty function body was not inlined")
  fun <Dialect : SqlIdiom> buildPretty(@ExoBuildFunctionLabel label: String): SqlCompiledAction<Input, Output> = errorCap("The buildPretty function body was not inlined")

  val buildFor: BuildFor<SqlCompiledAction<Input, Output>>
  val buildPrettyFor: BuildFor<SqlCompiledAction<Input, Output>>

  @ExoInternal
  override fun withNonStrictEquality(): SqlAction<Input, Output> = copy(params = params.withNonStrictEquality())

  @ExoInternal
  fun determinizeDynamics(): SqlAction<Input, Output> = DeterminizeDynamics().ofAction(this)

  // Don't need to do anything special in order to convert runtime, just call a function that the TransformProjectCapture can't see through
  fun dyanmic(): SqlAction<Input, Output> = this
}

data class SqlBatchAction<BatchInput, Input : Any, Output>(override val xr: XR.Batching, val batchParam: Sequence<BatchInput>, override val runtimes: RuntimeSet, override val params: ParamSet) : ContainerOfXR {
  fun show() = PrintMisc().invoke(this)

  @ExoInternal
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): SqlBatchAction<BatchInput, Input, Output> =
    copy(xr = xr as? XR.Batching ?: xrError("Failed to rebuild SqlBatchAction with XR of type ${xr::class} which was: ${xr.show()}"), runtimes = runtimes, params = params)

  @ExoInternal
  fun buildRuntime(dialect: SqlIdiom, label: String?, pretty: Boolean = false): SqlCompiledBatchAction<BatchInput, Input, Output> = run {
    val containerBuild = RuntimeBuilder(dialect, pretty).forBatching(this)
    val actionReturningKind = ActionReturningKind.fromActionXR(xr.action)
    SqlCompiledBatchAction(
      containerBuild.queryString, containerBuild.queryTokenized, true, xr.action.toActionKind(), actionReturningKind, batchParam, label,
      SqlCompiledBatchAction.DebugData(Phase.Runtime, { xr })
    )
  }

  fun <Dialect : SqlIdiom> build(): SqlCompiledBatchAction<BatchInput, Input, Output> = errorCap("The build function body was not inlined")
  fun <Dialect : SqlIdiom> build(@ExoBuildFunctionLabel label: String): SqlCompiledBatchAction<BatchInput, Input, Output> = errorCap("The build function body was not inlined")

  fun <Dialect : SqlIdiom> buildPretty(): SqlCompiledBatchAction<BatchInput, Input, Output> = errorCap("The buildPretty function body was not inlined")
  fun <Dialect : SqlIdiom> buildPretty(@ExoBuildFunctionLabel label: String): SqlCompiledBatchAction<BatchInput, Input, Output> = errorCap("The buildPretty function body was not inlined")

  val buildFor: BuildFor<SqlCompiledBatchAction<BatchInput, Input, Output>>
  val buildPrettyFor: BuildFor<SqlCompiledBatchAction<BatchInput, Input, Output>>

  @ExoInternal
  override fun withNonStrictEquality(): SqlBatchAction<BatchInput, Input, Output> = copy(params = params.withNonStrictEquality())

  @ExoInternal
  fun determinizeDynamics(): SqlBatchAction<BatchInput, Input, Output> = DeterminizeDynamics().ofBatchAction(this)

  // Don't need to do anything special in order to convert runtime, just call a function that the TransformProjectCapture can't see through
  @ExoInternal
  fun dyanmic(): SqlBatchAction<BatchInput, Input, Output> = this
}
