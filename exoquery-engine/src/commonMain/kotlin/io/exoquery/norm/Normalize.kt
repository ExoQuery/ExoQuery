package io.exoquery.norm

import io.exoquery.lang.QueryData
import io.exoquery.printing.HasPhasePrinting
import io.exoquery.util.TraceConfig
import io.exoquery.util.TraceType
import io.exoquery.util.Tracer
import io.exoquery.xr.*
import io.exoquery.xr.XR.*
import io.exoquery.xr.XR

class Normalize(override val traceConf: TraceConfig, val disableApplyMap: Boolean, queryData: QueryData) : StatelessTransformer, HasPhasePrinting {

  override val traceType: TraceType = TraceType.Normalizations
  override val trace: Tracer = Tracer(traceType, traceConf, 1)

  val DealiasPhase by lazy { DealiasApply(traceConf) }
  val AvoidAliasConflictPhase by lazy { AvoidAliasConflictApply(traceConf) }
  val NormalizeNestedStructuresPhase by lazy { NormalizeNestedStructures(this) }
  val SymbolicReductionPhase by lazy { SymbolicReduction(traceConf, queryData.containsFlatUnits) }
  val AdHocReductionPhase by lazy { AdHocReduction(traceConf) }
  val OrderTermsPhase by lazy { OrderTerms(traceConf) }

  // Quill originally did this at the root-level. I think it is fine
  // to just do it for expressions because normalizations will propagate down to them anyway but I'm not 100% sure
  override fun invoke(xr: Expression): Expression =
    super.invoke(BetaReduction(xr).asExpr())

  override operator fun invoke(q: Query): Query =
    trace("Avoid Capture and Normalize $q into:") andReturn {
      val reduced = BetaReduction(q).asQuery()
      norm(DealiasPhase(reduced))
    }

  val applyMapInterp = Tracer(TraceType.ApplyMap, traceConf, 1)
  val applyMapInstance = ApplyMap(traceConf)
  fun ApplyMapPhase(q: Query): Query? {
    // For logging that ApplyMap has been disabled
    return if (disableApplyMap) {
      applyMapInstance.trace("ApplyMap phase disabled. Not executing on: $q").andLog()
      null
    } else {
      applyMapInstance(q)
    }
  }

  private fun norm(q: Query): Query {
    return NormalizeNestedStructuresPhase(q)?.let {
      demarcate("NormalizeNestedStructures (COMPLETED)", it)
      norm(it)
    } ?: ApplyMapPhase(q)?.let {
      demarcate("ApplyMap", it)
      norm(it)
    } ?: SymbolicReductionPhase(q)?.let {
      demarcate("SymbolicReduction (COMPLETED)", it)
      norm(it)
    } ?: AdHocReductionPhase(q)?.let {
      demarcate("AdHocReduction (COMPLETED)", it)
      norm(it)
    } ?: OrderTermsPhase(q)?.let {
      demarcate("OrderTerms (COMPLETED)", it)
      norm(it)
    }
    ?: q
  }

  // This is the original Quill code that was used to recursively normalize
  //  @tailrec
  //  private def norm(q: Query): Query =
  //    q match {
  //      case NormalizeNestedStructuresPhase(query) =>
  //        demarcate("NormalizeNestedStructures")(query)
  //        norm(query)
  //      case ApplyMapPhase(query) =>
  //        demarcate("ApplyMap")(query)
  //        norm(query)
  //      case SymbolicReductionPhase(query) =>
  //        demarcate("SymbolicReduction")(query)
  //        norm(query)
  //      case AdHocReductionPhase(query) =>
  //        demarcate("AdHocReduction")(query)
  //        norm(query)
  //      case OrderTermsPhase(query) =>
  //        demarcate("OrderTerms")(query)
  //        norm(query)
  //      case other =>
  //        other
  //    }
}
