package io.exoquery

import io.exoquery.annotation.ExoBuildFunctionLabel
import io.exoquery.annotation.ExoInternal
import io.exoquery.printing.PrintMisc
import io.exoquery.lang.SqlIdiom
import io.exoquery.xr.RuntimeBuilder
import io.exoquery.xr.XR
import io.exoquery.xr.toActionKind

data class SqlAction<Input, Output> @ExoInternal constructor(val xrMaker: () -> XR.Action, override val runtimes: RuntimeSet, override val params: ParamSet) : ContainerOfActionXR {
  @ExoInternal
  override val xr: XR.Action by lazy { xrMaker() }

  // Materialize the id only lazily since we don't want to actually compute the xr unless needed (since when coming
  // from the compiled form it requires deserialization)
  private data class Id(val xr: XR.Action, val runtimes: RuntimeSet, val params: ParamSet)
  private val id: Id by lazy { Id(xr, runtimes, params) }
  override fun equals(other: Any?): Boolean =
    other is SqlAction<*, *> && this.id == other.id
  override fun hashCode(): Int = id.hashCode()
  override fun toString(): String = "SqlAction(${xr}, runtimes=$runtimes, params=$params)"

  fun show() = PrintMisc().invoke(this)

  companion object {
    @ExoInternal
    internal fun <Input, Output> fromPackedXR(packedXR: String, runtimes: RuntimeSet = RuntimeSet.Empty, params: ParamSet = ParamSet.Empty): SqlAction<Input, Output> =
      SqlAction({ unpackAction(packedXR) }, runtimes, params)

    @ExoInternal
    operator fun <Input, Output> invoke(xr: XR.Action, runtimes: RuntimeSet = RuntimeSet.Empty, params: ParamSet = ParamSet.Empty): SqlAction<Input, Output> =
      SqlAction({ xr }, runtimes, params)
  }

  @ExoInternal
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): SqlAction<Input, Output> =
    copy(xrMaker = { xr as? XR.Action ?: xrError("Failed to rebuild SqlAction with XR of type ${xr::class} which was: ${xr.show()}") }, runtimes = runtimes, params = params)

  @ExoInternal
  fun buildRuntime(dialect: SqlIdiom, label: String?, pretty: Boolean = false): SqlCompiledAction<Input, Output> = run {
    val containerBuild = RuntimeBuilder(dialect, pretty).forAction(this)
    val actionReturningKind = ActionReturningKind.fromActionXR(xr)
    SqlCompiledAction(
      containerBuild.queryString, containerBuild.queryTokenized, true, xr.toActionKind(), actionReturningKind, label,
      SqlCompiledAction.DebugData(Phase.Runtime, { this.xr })
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
  @ExoInternal
  fun dynamic(): SqlAction<Input, Output> = this
}
