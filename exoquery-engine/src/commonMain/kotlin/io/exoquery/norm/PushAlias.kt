package io.exoquery.norm

import io.exoquery.util.TraceConfig
import io.exoquery.util.TraceType
import io.exoquery.util.Tracer
import io.exoquery.xr.XR.*
import io.exoquery.xr.XR.Map // Make sure to explicitly have this import or Scala will use Map the collection
import io.exoquery.xr.XR
import io.exoquery.xr.*
import io.exoquery.xr.BetaReduction
import io.exoquery.xr.copy.*
import io.exoquery.xrError

/**
 * PushAlias Phase - Fixes Duplicate Subquery Alias Bug
 *
 * This normalization phase solves a critical bug where lambda parameter names in composeFrom.join
 * operations were being ignored, causing multiple filtered subqueries to receive the same alias ("it").
 *
 * THE BUG:
 * When using composeFrom.join with filtered subqueries and explicit lambda parameter names:
 *
 * ```kotlin
 * @SqlFragment
 * fun A.activeB() = sql {
 *   composeFrom.join(Table<B>().filter { it.status == "active" }) { b -> b.id == this@activeB.bId }
 * }
 *
 * @SqlFragment
 * fun A.activeC() = sql {
 *   composeFrom.join(Table<C>().filter { it.status == "active" }) { c -> c.id == this@activeC.cId }
 * }
 *
 * val query = sql.select {
 *   val a = from(Table<A>())
 *   val b = from(a.activeB())
 *   val c = from(a.activeC())
 *   Result(a.id, b.id, c.id)
 * }
 * ```
 *
 * Generated INCORRECT SQL (before fix):
 * ```sql
 * SELECT a.id AS aId, it.id AS bId, it.id AS cId
 * FROM A a
 *   INNER JOIN (
 *     SELECT it.id, it.status FROM B it WHERE it.status = 'active'
 *   ) AS it ON it.id = a.bId
 *   INNER JOIN (
 *     SELECT it.id, it.status FROM C it WHERE it.status = 'active'
 *   ) AS it ON it.id = a.cId
 * ```
 *
 * Both subqueries got alias "it" despite lambda parameters being named "b" and "c".
 *
 * THE ROOT CAUSE:
 * The lambda parameter names ("b", "c") were only used in the ON clause but not pushed into
 * the filtered subqueries themselves. Each Table<B>().filter and Table<C>().filter used the
 * default "it" identifier, which wasn't replaced with the join's lambda parameter name.
 *
 * THE FIX:
 * The fix is simple: ensure that FlatMap invokes the PushAlias transformer on its body.
 * When the body is a FlatJoin, the existing FlatJoin case (line 164-169) already handles
 * pushing the FlatJoin's id into its head query.
 *
 * How it works step-by-step for `a.activeB()`:
 *
 * Step 1: Initial XR structure (simplified):
 * ```
 * FlatMap(
 *   head = Table<A>(),
 *   id = Ident("a"),
 *   body = FlatJoin(
 *     head = Filter(
 *       head = Table<B>(),
 *       id = Ident("it"),
 *       body = Expr(it.status == "active")
 *     ),
 *     id = Ident("b"),    // <-- This is the lambda parameter name!
 *     on = Expr(b.id == a.bId)
 *   )
 * )
 * ```
 *
 * Step 2: FlatMap processes its body, invoking PushAlias on the FlatJoin
 * ```
 * invoke(body)  // body is FlatJoin with id="b"
 * ```
 *
 * Step 3: FlatJoin case pushes its id ("b") into its head query:
 * ```
 * PushAlias(id="b", traceConfig).invoke(head)  // head is the Filter
 * ```
 *
 * Step 4: Filter case applies beta reduction, replacing "it" with "b":
 * ```
 * Filter(
 *   head = Table<B>(),
 *   id = Ident("b"),     // <-- Changed from "it" to "b"!
 *   body = Expr(b.status == "active")  // <-- "it.status" became "b.status"
 * )
 * ```
 *
 * Step 5: Result - the subquery now uses "b" throughout:
 * ```sql
 * SELECT b.id, b.status FROM B b WHERE b.status = 'active'
 * ```
 *
 * The same process happens for `a.activeC()` but with id="c", producing distinct aliases.
 *
 * After this phase runs, the correct SQL is generated:
 * ```sql
 * SELECT a.id AS aId, b.id AS bId, c.id AS cId
 * FROM A a
 *   INNER JOIN (
 *     SELECT b.id, b.status FROM B b WHERE b.status = 'active'
 *   ) AS b ON b.id = a.bId
 *   INNER JOIN (
 *     SELECT c.id, c.status FROM C c WHERE c.status = 'active'
 *   ) AS c ON c.id = a.cId
 * ```
 *
 * Now each subquery has a distinct alias matching its lambda parameter name.
 */
data class PushAlias(override val state: XR.Ident?, val traceConfig: TraceConfig) : StatefulTransformer<Ident?> {
  val trace: Tracer =
    Tracer(TraceType.Standard, traceConfig, 1)

  override operator fun invoke(q: Query): Pair<Query, StatefulTransformer<Ident?>> =
    with(q) {
      when (this) {

        // If the FlatMap head is a FlatJoin, push the alias into it
        // For example:
        // sql.select {
        //   val x = from(...)
        //   val a = join(expression /* `shove` alias into here */) { alias -> ... } <- this case
        //   val b = join(...) { ... }
        // }
        is FlatMap if head is FlatJoin -> {
          val (hn, _) = PushAlias(id, traceConfig).invoke(head)
          val (bn, _) = PushAlias(null, traceConfig).invoke(body)
          FlatMap.cs(hn, id, bn) to PushAlias(null, traceConfig)
        }
        // I.e. a tail-position map after a FlatJoin
        // For example:
        // sql.select {
        //   val a = from(...)
        //   val b = join(expression /* `shove` alias into here */) { alias -> ... } <- this case
        // }
        is Map if head is FlatJoin -> {
          // Ignore FlatMap - don't push aliases through it
          val (hn, _) = PushAlias(id, traceConfig).invoke(head)
          val (bn, _) = PushAlias(null, traceConfig).invoke(body)
          Map.cs(hn, id, bn) to PushAlias(null, traceConfig)
        }

        // In general we do not carry aliases across FlatMap boundaries in the general case
        is FlatMap -> {
          val (hn, _) = PushAlias(null, traceConfig).invoke(head)
          val (bn, _) = PushAlias(null, traceConfig).invoke(body)
          FlatMap.cs(hn, id, bn) to PushAlias(null, traceConfig)
        }

        is ConcatMap -> {
          // Ignore ConcatMap - don't push aliases through it
          val (hn, _) = invoke(head)
          ConcatMap.cs(hn, id, body) to PushAlias(null, traceConfig)
        }

        is Map -> {
          val (a, b, c, t) = pushAlias(head, id, body)
          Map.cs(a, b, c) to t
        }

        is Filter -> {
          val (a, b, c, t) = pushAlias(head, id, body)
          Filter.cs(a, b, c) to t
        }

        is SortBy -> {
          val pushedAlias = state
          if (pushedAlias != null) {
            val retypedAlias = pushedAlias.copy(type = id.type)
            trace("PushAlias (SortBy) $id replaced with $retypedAlias").andLog()
            // Replace id with pushedAlias in criteria
            val cn = criteria.map { ord -> ord.transform { BetaReduction(it, id to retypedAlias).asExpr() } }
            val (an, ant) = PushAlias(pushedAlias, traceConfig).invoke(head)
            SortBy.cs(an, retypedAlias, cn) to ant
          } else {
            val (an, ant) = invoke(head)
            SortBy.cs(an, id, criteria) to ant
          }
        }

        is DistinctOn -> {
          val (a, b, c, t) = pushAlias(head, id, by)
          DistinctOn.cs(a, b, c) to t
        }

        is Take -> {
          val (an, ant) = invoke(head)
          Take.cs(an, num) to ant
        }

        is Limit -> {
          val (an, ant) = invoke(head)
          Limit.cs(an, num) to ant
        }

        is Drop -> {
          val (an, ant) = invoke(head)
          Drop.cs(an, num) to ant
        }

        is Union -> {
          val (an, _) = invoke(a)
          val (bn, _) = invoke(b)
          Union.cs(an, bn) to PushAlias(null, traceConfig)
        }

        is UnionAll -> {
          val (an, _) = invoke(a)
          val (bn, _) = invoke(b)
          UnionAll.cs(an, bn) to PushAlias(null, traceConfig)
        }

        is FlatJoin -> {
          if (state != null) {
            // If there is an external alias (that came from a FlatMap(FlatJoin, id, ...) then push that inside
            // This is the case of: select { val a = from(tableA.join(...){ *it* -> ... }); val b = from(tableB.join(...){ *it* -> ... }); ... }
            // i.e. some kind of generic alias that would clash when dealiased out of the inner joins
            val (a, b, c, _) = pushAlias(head, id, on)
            FlatJoin.cs(a, b, c) to PushAlias(null, traceConfig)
          }
          else {
            // Otherwise push the id into the head query, FlatJoin is the source of alias pushing
            val (headn, _) = PushAlias(id, traceConfig).invoke(head)
            FlatJoin.cs(headn, id, on) to PushAlias(null, traceConfig)
          }
        }

        is Entity, is Distinct, is Nested, is FlatFilter, is FlatHaving, is FlatSortBy, is FlatGroupBy, is Free, is ExprToQuery, is TagForSqlQuery, is GlobalCall, is MethodCall -> {
          this to PushAlias(null, traceConfig)
        }

        is CustomQueryRef -> {
          val (customQuery, newState) = customQuery.handleStatefulTransformer(this@PushAlias)
          CustomQueryRef.cs(customQuery) to newState
        }
        is FunctionApply, is FunctionN, is Ident ->
          xrError("PushAlias not supported (it should have been done already) for: ${this.showRaw()}")
      }
    }

  data class PushAliasResultA(val a: Query, val b: Ident, val c: Expression, val newState: StatefulTransformer<Ident?>)
  data class PushAliasResultB(val a: Query, val b: Ident, val c: Query, val newState: StatefulTransformer<Ident?>)

  private fun pushAlias(a: Query, b: Ident, c: Expression): PushAliasResultA {
    val pushedAlias = state
    return when {
      pushedAlias != null && !pushedAlias.isUnused() -> {
        val retypedAlias = pushedAlias.copy(type = b.type)
        trace("PushAlias (Q/Expr) $b into $retypedAlias").andLog()
        // Replace b with pushedAlias in the expression c, then continue pushing into a
        val (an, t) = PushAlias(pushedAlias, traceConfig).invoke(a)
        PushAliasResultA(an, retypedAlias, BetaReduction(c, b to retypedAlias).asExpr(), t)
      }
      else -> {
        val (an, t) = invoke(a)
        PushAliasResultA(an, b, c, t)
      }
    }
  }

  private fun pushAlias(a: Query, b: Ident, c: Query): PushAliasResultB {
    val pushedAlias = state
    return when {
      pushedAlias != null && !pushedAlias.isUnused() -> {
        val retypedAlias = pushedAlias.copy(type = b.type)
        trace("PushAlias (Q/Q) $b into $retypedAlias").andLog()
        // Replace b with pushedAlias in the query c, then continue pushing into a
        val (an, t) = PushAlias(pushedAlias, traceConfig).invoke(a)
        PushAliasResultB(an, retypedAlias, BetaReduction.ofQuery(c, b to retypedAlias), t)
      }
      else -> {
        val (an, t) = invoke(a)
        PushAliasResultB(an, b, c, t)
      }
    }
  }
}

class PushAliasApply(val traceConfig: TraceConfig) {
  operator fun invoke(query: Query): Query =
    PushAlias(null, traceConfig)(query).first
}
