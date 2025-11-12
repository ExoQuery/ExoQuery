package io.exoquery

import io.exoquery.annotation.ExoInternal
import io.exoquery.xr.XR

@ExoInternal
sealed interface ContainerOfXR {
  val xr: XR

  // I.e. runtime containers that are used in the expression (if any)
  val runtimes: RuntimeSet
  val params: ParamSet

  fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): ContainerOfXR
  fun withNonStrictEquality(): ContainerOfXR
}

// Less fungible i.e. always top-level and only for actions
@ExoInternal
sealed interface ContainerOfActionXR : ContainerOfXR {
  override val xr: XR.Action
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): ContainerOfActionXR
  override fun withNonStrictEquality(): ContainerOfActionXR
}

// Specifically for fungible XR Query and Expression types that are composable (e.g. that can be in a RuntimeSet)
@ExoInternal
sealed interface ContainerOfFunXR : ContainerOfXR {
  override val xr: XR.U.QueryOrExpression
  override fun rebuild(xr: XR, runtimes: RuntimeSet, params: ParamSet): ContainerOfFunXR
  override fun withNonStrictEquality(): ContainerOfFunXR
}
