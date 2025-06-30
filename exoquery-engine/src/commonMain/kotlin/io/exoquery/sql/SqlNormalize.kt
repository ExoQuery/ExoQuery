package io.exoquery.sql

import io.exoquery.norm.AvoidAliasConflictApply
import io.exoquery.norm.ExpandProductNullChecks
import io.exoquery.norm.Normalize
import io.exoquery.norm.NormalizeCustomQueries
import io.exoquery.norm.RepropagateTypes
import io.exoquery.printing.HasPhasePrinting
import io.exoquery.util.TraceConfig
import io.exoquery.util.TraceType
import io.exoquery.util.Tracer
import io.exoquery.xr.*
import io.exoquery.xr.XR.*
import io.exoquery.xr.XR

class SqlNormalize(
  val concatBehavior: ConcatBehavior = ConcatBehavior.AnsiConcat,
  val equalityBehavior: EqualityBehavior = EqualityBehavior.AnsiEquality,
  override val traceConf: TraceConfig = TraceConfig.empty,
  val disableApplyMap: Boolean = false
) : HasPhasePrinting {
  override val traceType = TraceType.Normalizations
  override val trace by lazy { Tracer(traceType, traceConf, 1) }

  val AvoidAliasConflictPhase by lazy { AvoidAliasConflictApply(traceConf) }
  val NormalizePhase = Normalize(traceConf, disableApplyMap)
  val RepropagateTypesPhase = RepropagateTypes(traceConf)
  val ExpandProductNullChecks = io.exoquery.norm.ExpandProductNullChecks
  val NormalizeCustomQueriesPhase = NormalizeCustomQueries

  private val root = { it: Query -> it }
  private val normalize =
    root
      .andThen("Beginning-Normalization") {
        it
      }
      .andThen("NormalizeCustomQueries") {
        // Make sure to do this before the initial beta-reduction because custom queries
        // can contain aliases would not otherwise be reduced by the beta-reducer.
        // See the note for CustomQuery (in XR.scala) for more information.
        NormalizeCustomQueriesPhase(it)
      }
      .andThen("AvoidAliasConflictPhase") {
        AvoidAliasConflictPhase(it)
      }
      .andThen("BetaReduce-Initial") {
        BetaReduction.ofQuery(it)
      }
      //.andThen("ExpandProductNullChecks") {
      //  ExpandProductNullChecks(it)
      //}
      .andThen("RepropagateTypes") {
        RepropagateTypesPhase(it)
      }
      .andThen("Normalize") {
        NormalizePhase(it)
      }
  // TODO ExpandDistinct

  inline fun ((Query) -> Query).andThen(phaseTitle: String, crossinline f: (Query) -> Query): (Query) -> Query = { qRaw ->
    // Too much noise when both before and after the phase are printed
    //demarcate(phaseTitle, q)
    val label = traceConf.phaseLabel
    val labelText = if (label != null) " (${label})" else ""
    val q = this(qRaw)

    demarcate("${phaseTitle} ${labelText}", q)
    val output = f(q)

    demarcate("${phaseTitle} (COMPLETED) ${labelText}", output)
    output
  }

  operator fun invoke(q: Query): Query = normalize(q)
}
