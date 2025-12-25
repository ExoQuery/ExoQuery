package io.exoquery.norm

import io.decomat.*
import io.exoquery.util.TraceConfig
import io.exoquery.util.TraceType
import io.exoquery.util.Tracer
import io.exoquery.xr.XR
import io.exoquery.xr.XR.*
import io.exoquery.xr.*
import io.exoquery.xr.copy.*
import kotlin.invoke

class CrunchFlatJoins(val traceConfig: TraceConfig) {

  val trace: Tracer =
    Tracer(TraceType.CrunchFlatJoins, traceConfig, 1)

  operator fun invoke(q: Query): Query? =
    on(q).match(
      /*
       * FlatMap(Map(FlatJoin(...), id, projection), bindId, body) =>
       *   FlatMap(FlatJoin(...), id, BetaReduction(body, bindId -> projection))
       *
       * This transformation extracts FlatJoin from Map wrappers and pushes the projection
       * into the FlatMap body via beta reduction.
       *
       * EXAMPLE:
       * Input:
       *   FlatMap(
       *     Map(FlatJoin(Table<Order>), o, Tuple(o, free("rand()"))),
       *     pair,
       *     body
       *   )
       *
       * Output:
       *   FlatMap(
       *     FlatJoin(Table<Order>), o,
       *     BetaReduction(body, pair -> Tuple(o, free("rand()")))
       *   )
       */
      case(XR.FlatMap[XR.Map[Is<XR.FlatJoin>(), Is()], Is()]).thenThis { (a, b, c), d, e ->
        val er = BetaReduction.ofQuery(e, d to c)
        trace("CrunchFlatJoins: FlatMap(Map(FlatJoin)) for $q") andReturn { FlatMap.cs(a, b, er) }
      },

      /*
       * Filter(FlatJoin, filterExpr) - Handle filtered joins
       *
       * This case handles FlatMap(Filter(FlatJoin(...), id, filterExpr), bindId, body) where
       * a filter is applied after a join operation. This pattern needs special handling because
       * Filter(FlatJoin) creates an intermediate structure that should be merged into the join's
       * ON clause for optimal SQL generation.
       *
       * If this phase does not exist there could potentially be an issue
       * where the SqlQueryModel ends up creating a "FROM INNER JOIN" query. There is an additional
       * control against that happening in SqlQueryModel where it does a isFilteredFlatJoin check and then
       * inner-nests the FlatJoin clause Flipping Filter(FlatJoin) to FlatJoin(Filter) but it is
       * less efficient because that results in a nested query.
       *
       * EXAMPLE:
       * Input:
       *   FlatMap(
       *     Filter(
       *       FlatJoin(Table<OrderItem>, oi, oi.order_id == o.order_id),
       *       oi,
       *       o.status == "PAID"
       *     ),
       *     oi,
       *     Map(oi.qty, _, _)
       *   )
       *
       * Output:
       *   FlatMap(
       *     FlatJoin(
       *       Table<OrderItem>, oi,
       *       (oi.order_id == o.order_id) AND (o.status == "PAID")  // <- merged filter
       *     ),
       *     oi,
       *     Map(oi.qty, _, _)
       *   )
       *
       */
      case(XR.FlatMap[XR.Filter[Is<XR.FlatJoin>(), Is()], Is()]).thenThis { (a, b, c), d, e ->
        val additionalFilter = BetaReduction(c, b to a.id).asExpr()
        val flatJoinWithAdditionalFilter =
          FlatJoin.csf(a.head, a.id, a.on _And_ additionalFilter)(a)
        trace("CrunchFlatJoins: FlatMap(Filter(FlatJoin)) for $q") andReturn { FlatMap.cs(flatJoinWithAdditionalFilter, d, e) }
      },



      /**
       * The Filter(FlatFilter, _, _) case
       *
       * This is my own transformation as opposed to being from Wadler's paper. It represents
       * a situation where a Filter clause is pushed deeper and deeper in the query (see the next transformation)
       * until eventually it reaches a FlatUnit and you get something like Filter(FlatUnit, x, ...). In this kind
       * of situation the `x` is meaningless because FlatUnit returns a unit-type so it can be ignored.
       * Therefore we can just merge the Filter into a FlatFilter.
       *
       * Note: We use (head.by _And_ body) to preserve the original filter order, so that:
       * Filter(FlatFilter(a), _, b) becomes FlatFilter(a AND b), not FlatFilter(b AND a)
       *
       * Also note that this transformation RELIES on dealiasing happening first, otherwise would have something like this:
       *
       * where { expr }.filter { x -> x.id == 123 }
       * -> where { expr && x.id == 123 } // x would be an unmoored identifier
       *
       * Fortunately we have dealising which should have already gotten rid of `x` for a surrounding variable
       * because the whole expression should be in a surrounding flatMap
       *
       * Assuming we have this:
       * where { expr }.filter { x -> x.id == 123 }.flatMap { y -> ... }
       * Dealiasing would have already done something like this:
       * where { expr }.filter { y -> y.id == 123 }.flatMap { y -> ... }
       */
      case(XR.Filter[XR.FlatFilter[Is()], Is()]).then { (flatFilterBy), id, filter ->
        XR.FlatFilter(flatFilterBy _And_ filter)
      }


    )

}
