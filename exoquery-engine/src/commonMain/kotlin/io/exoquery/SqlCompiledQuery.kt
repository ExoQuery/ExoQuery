package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.lang.SqlQueryModel
import io.exoquery.lang.Token
import io.exoquery.xr.XR

/**
 * This is a runnable ExoQuery SqlQuery instance. The `value` variable contains the SQL string.
 * In order to execute it import a exoquery runner project e.g. exoquery-runner-jdbc, then create a controller and run it. For example:
 * ```
 * val ds: DataSource = ...
 * val controller = JdbcControllers.Postgres(ds)
 * val myQuery: SqlQuery<Person> = sql { Table<Person>().filter { p -> p.name == "Joe" } }
 * val result: List<Person> = myQuery.buildFor.Postgres().runOn(controller)
 * ```
 */
data class SqlCompiledQuery<T> @ExoInternal constructor(override val value: String, val tokenMaker: () -> Token, val needsTokenization: Boolean, val label: String?, val debugData: SqlCompiledQuery.DebugData) : ExoCompiled() {
  @ExoInternal
  override val token: Token by lazy { tokenMaker() }

  @ExoInternal
  override val params: List<Param<*>> by lazy { token.extractParams() }
  @ExoInternal
  override fun originalXR(): XR = debugData.originalXR()

  // Similar concept tot the SqlQuery/SqlExpression.determinizeDynamics but it does not need to consider any nesting constructs
  // because the Params in the `params` variable are already determined to be the complete set in the tokenization
  // (determined by Lifter.liftToken + realization for compile-time and buildRuntime + realization for runtime)
  @ExoInternal
  override fun determinizeDynamics(): SqlCompiledQuery<T> = run {
    val token = determinizedToken()
    val sql = determinizedToken().build()
    this.copy(value = sql, tokenMaker = { token })
  }

  @ExoInternal
  data class DebugData(val phase: Phase, val originalXR: () -> XR.Query, val originalQuery: () -> SqlQueryModel)
}
