package io.exoquery.norm

import io.decomat.*

import io.exoquery.xr.StatelessTransformer
import io.exoquery.xr.XR
import io.exoquery.xr.contains
import io.exoquery.xr.oneSideIs

object ExpandReducibleNullChecks : StatelessTransformer {
  override fun invoke(xr: XR.Expression): XR.Expression =
    on(xr).match(
      // TODO look into the actual use-cases of product null checks to see if they are needed
      //      normally doing nullableRow==null will intentionally blow up in the parser because only
      //      columns can be checked this way. Are there any other real use-cases that exist?
      //
      //case(XR.BinaryOp.EqEq[IsTypeProduct(), NullXR()]).thenThis { core, nullCheck ->
      //  val newCore = invoke(core)
      //  val props =
      //    ProtractQuat(false)(core.type as XRType.Product, newCore).map { (prop, _) ->
      //      prop _NotEq_ XR.Const.Null(nullCheck.loc)
      //    }
      //  props.reduce { a: XR.Expression, b: XR.Expression -> a _And_ b }
      //},
      /*
       * Something like `if (x is null) null else nonConditioningExpr(x)`
       * What this means is that situations where you have something like `if (x is null) null else x || 'foo'`
       * then you can do just `x || 'foo'` because of nature of how SQL works (i.e. x being null turns the whole expression null).
       * The only situation where this doesn't work is if the orElse expression contains another conditional
       * for example `if (x is null) null else (if (x is null) then y else z)` since in that case the whole expression
       * or if we have a dialect that supports boolean like postgres where you've can have something like `if (x is null) null else (x IS null) AND blah`
       * where `x IS null` is actually a value that can be returned.
       * So we just check if the orElse expression has `x IS null`
       */
      case(XR.When.ifX_isK_thenKelseY[Is(), Is<XR.Const.Null>(), Is()]).then { x, orElse ->
        if (orElse.containsXandNoWhens(x))
          invoke(orElse)
        else
          super.invoke(xr)
      },
      case(XR.When.ifX_isNotK_thenYelseK[Is(), Is<XR.Const.Null>(), Is()]).thenThis { x, then ->
        if (then.containsXandNoWhens(x))
          invoke(then)
        else
          super.invoke(xr)
      }
    ) ?: super.invoke(xr)

  fun XR.containsXandNoWhens(x: XR.Expression) =
    this.contains(x) && !this.containsElementType { it is XR.BinaryOp && it.oneSideIs { it is XR.Const.Null } && it.oneSideIs(x) }
}
