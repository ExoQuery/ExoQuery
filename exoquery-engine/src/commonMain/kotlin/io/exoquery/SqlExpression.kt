package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.printing.PrintMisc
import io.exoquery.xr.XR

data class SqlExpression<T> @ExoInternal constructor(val xrMaker: () -> XR.Expression, override val runtimes: RuntimeSet, override val params: ParamSet) : ContainerOfFunXR {
  @ExoInternal
  override val xr: XR.Expression by lazy { xrMaker() }

  // Materialize the id only lazily since we don't want to actually compute the xr unless needed (since when coming
  // from the compiled form it requires deserialization)
  private data class Id(val xr: XR.Expression, val runtimes: RuntimeSet, val params: ParamSet)
  private val id: Id by lazy { Id(xr, runtimes, params) }
  override fun equals(other: Any?): Boolean =
    other is SqlExpression<*> && this.id == other.id
  override fun hashCode(): Int = id.hashCode()
  override fun toString(): String = "SqlExpression(${xr}, runtimes=$runtimes, params=$params)"

  @ExoInternal
  fun determinizeDynamics(): SqlExpression<T> = DeterminizeDynamics().ofExpression(this)

  fun show() = PrintMisc().invoke(this)

  @ExoInternal
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): SqlExpression<T> =
    copy(xrMaker = { xr as? XR.Expression ?: xrError("Failed to rebuild SqlExpression with XR of type ${xr::class} which was: ${xr.show()}") }, runtimes = runtimes, params = params)

  @ExoInternal
  override fun withNonStrictEquality(): SqlExpression<T> =
    copy(params = params.withNonStrictEquality())

  companion object {
    @ExoInternal
    internal fun <T> fromPackedXR(packedXR: String, runtimes: RuntimeSet = RuntimeSet.Empty, params: ParamSet = ParamSet.Empty): SqlExpression<T> =
      SqlExpression({ unpackExpr(packedXR) }, runtimes, params)

    @ExoInternal
    operator fun <T> invoke(xr: XR.Expression, runtimes: RuntimeSet = RuntimeSet.Empty, params: ParamSet = ParamSet.Empty): SqlExpression<T> =
      SqlExpression({ xr }, runtimes, params)
  }
}
