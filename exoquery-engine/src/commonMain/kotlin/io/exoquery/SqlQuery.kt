package io.exoquery

import io.exoquery.annotation.ExoBuildFunctionLabel
import io.exoquery.annotation.ExoInternal
import io.exoquery.norm.NormalizeCustomQueries
import io.exoquery.printing.PrintMisc
import io.exoquery.lang.SqlIdiom
import io.exoquery.xr.RuntimeBuilder
import io.exoquery.xr.XR

data class SqlQuery<T> @ExoInternal constructor (val xrMaker: () -> XR.Query, override val runtimes: RuntimeSet, override val params: ParamSet) : ContainerOfFunXR {
  @ExoInternal
  override val xr: XR.Query by lazy { xrMaker() }

  // Materialize the id only lazily since we don't want to actually compute the xr unless needed (since when coming
  // from the compiled form it requires deserialization)
  private data class Id(val xr: XR.Query, val runtimes: RuntimeSet, val params: ParamSet)
  private val id: Id by lazy { Id(xr, runtimes, params) }
  override fun equals(other: Any?): Boolean =
    other is SqlQuery<*> && this.id == other.id
  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "SqlQuery(${xr}, runtimes=$runtimes, params=$params)"


  companion object {
    @ExoInternal
    internal fun <T> fromPackedXR(packedXR: String, runtimes: RuntimeSet = RuntimeSet.Empty, params: ParamSet = ParamSet.Empty): SqlQuery<T> =
      SqlQuery({ unpackQuery(packedXR) }, runtimes, params)

    @ExoInternal
    operator fun <T> invoke(xr: XR.Query, runtimes: RuntimeSet = RuntimeSet.Empty, params: ParamSet = ParamSet.Empty): SqlQuery<T> =
      SqlQuery({ xr }, runtimes, params)
  }

  @ExoInternal
  fun determinizeDynamics(): SqlQuery<T> = DeterminizeDynamics().ofQuery(this)

  // Don't need to do anything special in order to convert runtime, just call a function that the TransformProjectCapture can't see through
  @ExoInternal
  fun dynamic(): SqlQuery<T> = this

  fun show() = PrintMisc().invoke(this)

  @ExoInternal
  fun buildRuntime(dialect: SqlIdiom, label: String?, pretty: Boolean = false): SqlCompiledQuery<T> = run {
    val containerBuild = RuntimeBuilder(dialect, pretty).forQuery(this)
    SqlCompiledQuery(
      containerBuild.queryString, { containerBuild.queryTokenized }, containerBuild.queryTokenized.isStatic(), label,
      SqlCompiledQuery.DebugData(Phase.Runtime, { this.xr }, { containerBuild.queryModel })
    )
  }

  fun <Dialect : SqlIdiom> build(): SqlCompiledQuery<T> = errorCap("The build function body was not inlined")
  fun <Dialect : SqlIdiom> build(@ExoBuildFunctionLabel label: String): SqlCompiledQuery<T> = errorCap("The build function body was not inlined")

  fun <Dialect : SqlIdiom> buildPretty(): SqlCompiledQuery<T> = errorCap("The buildPretty function body was not inlined")
  fun <Dialect : SqlIdiom> buildPretty(@ExoBuildFunctionLabel label: String): SqlCompiledQuery<T> = errorCap("The buildPretty function body was not inlined")

  val buildFor: BuildFor<SqlCompiledQuery<T>>
  val buildPrettyFor: BuildFor<SqlCompiledQuery<T>>

  @ExoInternal
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): SqlQuery<T> = run {
    val newXR = xr as? XR.Query ?: xrError("Failed to rebuild SqlQuery with XR of type ${xr::class} which was: ${xr.show()}")
    copy(xrMaker = { newXR }, runtimes = runtimes, params = params)
  }

  @ExoInternal
  override fun withNonStrictEquality(): SqlQuery<T> = copy(params = params.withNonStrictEquality())
  fun normalizeSelects(): SqlQuery<T> = run {
    val newXR = NormalizeCustomQueries(xr)
    copy(xrMaker = { newXR })
  }
}
