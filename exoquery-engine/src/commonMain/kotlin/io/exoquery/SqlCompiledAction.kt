package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.lang.Token
import io.exoquery.printing.PrintMisc
import io.exoquery.xr.XR

data class SqlCompiledAction<Input, Output> @ExoInternal constructor(
  override val value: String,
  override val token: Token,
  val needsTokenization: Boolean,
  val actionKind: ActionKind,
  val actionReturningKind: ActionReturningKind,
  val label: String?,
  val debugData: DebugData
) : ExoCompiled() {
  @ExoInternal
  override val params: List<Param<*>> by lazy { token.extractParams() }
  @ExoInternal
  override fun originalXR(): XR = debugData.originalXR()

  @ExoInternal
  override fun determinizeDynamics(): SqlCompiledAction<Input, Output> =
    this.copy(token = determinizedToken())

  fun show() = PrintMisc().invoke(this)

  @ExoInternal
  data class DebugData(val phase: Phase, val originalXR: () -> XR.Action)
}
