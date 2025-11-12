package io.exoquery

import io.exoquery.annotation.ExoBuildFunctionLabel
import io.exoquery.annotation.ExoInternal
import io.exoquery.lang.SqlIdiom
import io.exoquery.printing.PrintMisc
import io.exoquery.xr.RuntimeBuilder
import io.exoquery.xr.XR
import io.exoquery.xr.toActionKind

data class SqlBatchAction<BatchInput, Input : Any, Output> @ExoInternal constructor(val xrMaker: () -> XR.Batching, val batchParam: Sequence<BatchInput>, override val runtimes: RuntimeSet, override val params: ParamSet) : ContainerOfXR {
 @ExoInternal
 override val xr: XR.Batching by lazy { xrMaker() }

 // Materialize the id lazily to avoid deserialization unless needed
 private data class Id<BatchInput>(val xr: XR.Batching, val batchParam: Sequence<BatchInput>, val runtimes: RuntimeSet, val params: ParamSet)
 private val id: Id<BatchInput> by lazy { Id(xr, batchParam, runtimes, params) }
 override fun equals(other: Any?): Boolean =
   other is SqlBatchAction<*, *, *> && this.id == other.id
 override fun hashCode(): Int = id.hashCode()
 override fun toString(): String = "SqlBatchAction(${xr}, batchParam=$batchParam, runtimes=$runtimes, params=$params)"

 fun show() = PrintMisc().invoke(this)

 companion object {
   @ExoInternal
   internal fun <BatchInput, Input : Any, Output> fromPackedXR(packedXR: String, batchParam: Sequence<BatchInput>, runtimes: RuntimeSet = RuntimeSet.Empty, params: ParamSet = ParamSet.Empty): SqlBatchAction<BatchInput, Input, Output> =
     SqlBatchAction({ unpackBatchAction(packedXR) }, batchParam, runtimes, params)

   @ExoInternal
   operator fun <BatchInput, Input : Any, Output> invoke(xr: XR.Batching, batchParam: Sequence<BatchInput>, runtimes: RuntimeSet = RuntimeSet.Empty, params: ParamSet = ParamSet.Empty): SqlBatchAction<BatchInput, Input, Output> =
     SqlBatchAction({ xr }, batchParam, runtimes, params)
 }

 @ExoInternal
 override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): SqlBatchAction<BatchInput, Input, Output> =
   copy(xrMaker = { xr as? XR.Batching ?: xrError("Failed to rebuild SqlBatchAction with XR of type ${xr::class} which was: ${xr.show()}") }, runtimes = runtimes, params = params)

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
 fun dynamic(): SqlBatchAction<BatchInput, Input, Output> = this
}
