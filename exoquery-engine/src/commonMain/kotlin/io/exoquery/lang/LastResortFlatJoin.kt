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

fun XR.Query.isMappedFlatJoin() =
  this is XR.Map && this.head is XR.FlatJoin

fun XR.Query.isSomeKindOfFlatJoin() =
  this is XR.FlatJoin || this.isFilteredFlatJoin() //|| this.isMappedFlatJoin()

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
    // map( join(B) { b -> a.id == b.fk } , bb -> (bb.x, bb.y) )
    // -> join( map(B, bb -> (bb.x, bb.y)) ) { b -> a.id == /* shoot, the mapping projection might not contain the data we need! */ }

    // TODO need to do the same thing for Map(FlatJoin) -> FlatJoin(Map).
    //      Map(FlatJoin(a, b, c), d, e) -> FlatJoin(Map(
  )
