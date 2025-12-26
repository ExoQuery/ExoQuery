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
      },


      /*
       * FlatJoin(Filter(...), id, on) - Merge filter into join ON clause
       *
       * This case handles join(source.filter { filterExpr }) { joinExpr } where a filter
       * is applied to the joined source before the join operation. The filter needs to be
       * merged into the join's ON clause via beta reduction.
       *
       * EXAMPLE:
       * Input:
       *   join(Table<Order>.filter { o -> o.status == "active" }) { o -> o.customerId == c.id }
       *   -> FlatJoin(Filter(Entity(Order), o, o.status == "active"), o2, o2.customerId == c.id)
       *
       * Output:
       *   FlatJoin(Entity(Order), o2, (o2.customerId == c.id) AND BetaReduction(o.status == "active", o -> o2))
       *   -> FlatJoin(Entity(Order), o2, (o2.customerId == c.id) AND (o2.status == "active"))
       */
      case(XR.FlatJoin[XR.Filter[Is(), Is()], Is()]).then { (source, fid, filter), jid, on ->
        XR.FlatJoin.csf(source, jid, on _And_ BetaReduction(filter, fid to jid).asExpr())(comp)
      },

      /*
       * FlatJoin(Map(...), id, on) - Flatten map on joined source
       *
       * This case handles join(Table.map { ... }) { joinExpr } where a map projection is
       * applied to the joined source. Without this transformation, SqlQueryModel would create
       * a nested subquery for the mapped source. Instead, we extract the underlying source
       * and merge the map's projection into the join's ON clause via beta reduction.
       *
       * This is particularly important when PushAlias is disabled, as the map's lambda parameter
       * (fid) will differ from the join's identifier (jid), requiring BetaReduction to align
       * the references in the join condition.
       *
       * EXAMPLE:
       * Input:
       *   join(Table<OrderItem>.map { it -> OrderItemMapped(it.id, it.orderId + 1, it.qty) }) { io -> io.orderId == r.orderId }
       *   -> FlatJoin(Map(Entity(OrderItem), it, OrderItemMapped(...)), io, io.orderId == r.orderId)
       *
       * Output:
       *   FlatJoin(Entity(OrderItem), io, BetaReduction(io.orderId == r.orderId, it -> io))
       *   -> FlatJoin(Entity(OrderItem), io, io.orderId == r.orderId)
       *
       * Without this transformation, SqlQueryModel would generate:
       *   FROM ... INNER JOIN (SELECT it.id, it.orderId + 1 AS orderId, it.qty FROM OrderItem it) AS io ON io.orderId = r.orderId
       *
       * With this transformation, we generate the flatter:
       *   FROM ... INNER JOIN OrderItem io ON io.orderId = r.orderId
       *
       * Note that this transformation is dangerous to do if the Detachable map has an impure function (e.g. `rand()`) since it can
       * be reduced to two call sites. If it was a case of Map(FlatJoin) where we have no choice but to perform the normalization
       * (since the whole query is invalid otherwise and SqlQueryModel would create a "FROM INNER JOIN" scenario), however since here
       * it is a case of FlatJoin(Map) which SqlQueryModel can faithfully make into a nested query we have the leeway to ensure
       * complete correctness before making the transformation.
       */
      case(XR.FlatJoin[DetachableMap[Is(), Is()], Is()]).then { (source, fid, filter), jid, on ->
        XR.FlatJoin.csf(source, jid, on _And_ BetaReduction(filter, fid to jid).asExpr())(comp)
      }
    )

}
