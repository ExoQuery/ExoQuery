package io.exoquery.norm

import io.decomat.*
import io.exoquery.lang.ProtractQuat
import io.exoquery.xr._NotEq_
import io.exoquery.xr._And_
import io.exoquery.xr.EqEq
import io.exoquery.xr.IsTypeProduct
import io.exoquery.xr.NullXR

import io.exoquery.xr.StatelessTransformer
import io.exoquery.xr.XR
import io.exoquery.xr.XRType

// Convert all instances of CustomQuery (that are convertable to regular XR).
object ExpandProductNullChecks : StatelessTransformer {
  override fun invoke(xr: XR.Expression): XR.Expression =
    on(xr).match(
      case(XR.BinaryOp.EqEq[IsTypeProduct(), NullXR()]).thenThis { core, nullCheck ->
        val newCore = invoke(core)
        val props =
          ProtractQuat(false)(core.type as XRType.Product, newCore).map { (prop, _) ->
            prop _NotEq_ XR.Const.Null(nullCheck.loc)
          }
        props.reduce { a: XR.Expression, b: XR.Expression -> a _And_ b }
      }
    ) ?: super.invoke(xr)
}
