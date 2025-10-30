package io.exoquery

import io.exoquery.annotation.ExoBuildFunctionLabel
import io.exoquery.annotation.ExoInternal
import io.exoquery.norm.NormalizeCustomQueries
import io.exoquery.printing.PrintMisc
import io.exoquery.lang.SqlIdiom
import io.exoquery.xr.RuntimeBuilder
import io.exoquery.xr.XR

data class SqlQuery<T>(override val xr: XR.Query, override val runtimes: RuntimeSet, override val params: ParamSet) : ContainerOfFunXR {
  @ExoInternal
  fun determinizeDynamics(): SqlQuery<T> = DeterminizeDynamics().ofQuery(this)

  // Don't need to do anything special in order to convert runtime, just call a function that the TransformProjectCapture can't see through
  fun dyanmic(): SqlQuery<T> = this

  fun show() = PrintMisc().invoke(this)

  /*
  Argument taking the name of a query: (also make buildPretty) to pretty-print it
  query.build<PostgresDialect>(, "GetStuffFromStuff")
  ------< GetStuffFromStuff >-----
  select ...

  (fail if there are duplicate names in a file)

  Add another capability: Annotation on the top of a file to put into different location:
  @file:ExoLocation("src/main/resources/queries")
   */

  fun buildRuntime(dialect: SqlIdiom, label: String?, pretty: Boolean = false): SqlCompiledQuery<T> = run {
    val containerBuild = RuntimeBuilder(dialect, pretty).forQuery(this)
    SqlCompiledQuery(
      containerBuild.queryString, containerBuild.queryTokenized, true, label,
      SqlCompiledQuery.DebugData(Phase.Runtime, { this.xr }, { containerBuild.queryModel })
    )
  }

  fun <Dialect : SqlIdiom> build(): SqlCompiledQuery<T> = errorCap("The build function body was not inlined")
  fun <Dialect : SqlIdiom> build(@ExoBuildFunctionLabel label: String): SqlCompiledQuery<T> = errorCap("The build function body was not inlined")

  fun <Dialect : SqlIdiom> buildPretty(): SqlCompiledQuery<T> = errorCap("The buildPretty function body was not inlined")
  fun <Dialect : SqlIdiom> buildPretty(@ExoBuildFunctionLabel label: String): SqlCompiledQuery<T> = errorCap("The buildPretty function body was not inlined")

  val buildFor: BuildFor<SqlCompiledQuery<T>>
  val buildPrettyFor: BuildFor<SqlCompiledQuery<T>>

  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): SqlQuery<T> =
    copy(xr = xr as? XR.Query ?: xrError("Failed to rebuild SqlQuery with XR of type ${xr::class} which was: ${xr.show()}"), runtimes = runtimes, params = params)

  override fun withNonStrictEquality(): SqlQuery<T> = copy(params = params.withNonStrictEquality())
  fun normalizeSelects(): SqlQuery<T> = copy(xr = NormalizeCustomQueries(xr))
}
