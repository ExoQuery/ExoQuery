package io.exoquery.lang

import io.decomat.Is
import io.decomat.Is.Companion.invoke
import io.decomat.case
import io.decomat.on
import io.exoquery.xr.BetaReduction
import io.exoquery.xr.BetaReduction.Companion.invoke
import io.exoquery.xr.XR
import io.exoquery.xr.get

fun XR.Query.isFilteredFlatJoin() =
  this is XR.Filter && this.head is XR.FlatJoin

fun XR.Query.isSomeKindOfFlatJoin() =
  this is XR.FlatJoin || this.isFilteredFlatJoin()

fun XR.Query.processSomeKindOfFlatJoin(): XR.FlatJoin =
  if (this is XR.FlatJoin)
    this
  else
    processLastResortFlatJoin() ?: error("Failed to flatten Filter with FlatJoin, this should be impossible")


private fun XR.Query.processLastResortFlatJoin() =
  on(this).match(
    case(XR.Filter[XR.FlatJoin[Is(), Is()], Is()]).then { (inner, jid, onExpr), id, filterExpr  ->
      val filter = XR.Filter(inner, jid, BetaReduction(filterExpr, id to jid).asExpr())
      XR.FlatJoin(compLeft.joinType, filter, jid, onExpr)
    }
    // Unfortunately the same last-resort application cannot be done in a Map(FlatJoin) case for the below reason:
    // map( join(B) { b -> a.id == b.fk } , bb -> (bb.x, bb.y) )
    // -> join( map(B, bb -> (bb.x, bb.y)) ) { b -> a.id == /* the mapping projection might not contain the data we need! */ }
  )
