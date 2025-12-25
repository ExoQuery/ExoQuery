package io.exoquery.norm

import io.decomat.*
import io.exoquery.util.TraceConfig
import io.exoquery.util.TraceType
import io.exoquery.util.Tracer
import io.exoquery.xr.XR
import io.exoquery.xr.XR.*
import io.exoquery.xr.*
import io.exoquery.xr.copy.*

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
      }
    )

}
