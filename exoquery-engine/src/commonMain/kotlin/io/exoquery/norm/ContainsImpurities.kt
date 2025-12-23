package io.exoquery.norm

import io.exoquery.xr.StatelessChecker
import io.exoquery.xr.XR
import io.exoquery.xr.XR.Expression
import io.exoquery.xr.XR.Free

private object ContainsImpurities: StatelessChecker {
  override fun check(xr: XR): Boolean = when (xr) {
    is XR.U.Call -> !xr.isPure()
    is XR.Window -> true
    is Free -> xr.pure == false
    else -> false
  }
}

fun XR.Query.hasImpurities() = ContainsImpurities(this)
fun Expression.hasImpurities() = ContainsImpurities(this)

private class ContainsElementType(val checker: (XR) -> Boolean): StatelessChecker {
  override fun check(xr: XR): Boolean =
    checker(xr)
}

fun XR.containsElementType(checker: (XR) -> Boolean) =
  ContainsElementType(checker)(this)

private object ContainsAggregations: StatelessChecker {
  override fun check(xr: XR): Boolean = when (xr) {
    is XR.U.Call -> xr.isAggregation()
    else -> false
  }
}

fun XR.Query.hasAggregations() = ContainsAggregations(this)
fun Expression.hasAggregations() = ContainsAggregations(this)
