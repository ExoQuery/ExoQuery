package io.exoquery.norm

import io.exoquery.xr.XR.*
import io.exoquery.xr.XR.Map
import io.exoquery.xr.*
import io.exoquery.xr.copy.*
import io.decomat.*
import io.exoquery.util.TraceConfig
import io.exoquery.util.TraceType
import io.exoquery.util.Tracer

// NOTE: Leaving Quill commneted-out code equivalents here for now for reference
class AdHocReduction(val traceConfig: TraceConfig) {

  val trace: Tracer =
    Tracer(TraceType.Normalizations, traceConfig, 1)

  /**
   * This:
   * ```
   * You cannot use this pattern if the body of the FlatMap has a head that is a FlatJoin. For example:
   * ```
   * sql {
   *   select {
   *     val p = from(Table<Person>())
   *     val a = joinLeft(Table<Address>()) { it.ownerId == p.id }
   *     p to a
   *   }.filter { ccc -> ccc.first.name == "Main St" }
   * }
   * ```
   *
   * Same kind of thing if there is a flatFilter
   * ```
   * sql.select {
   *   val p = from(Table<Person>())
   *   where { p.age > 18 }
   *   p
   * }.filter { ccc -> ccc.name == "Main St" }
   * ```
   *
   * The latter creates a tree that looks something like this:
   * ```
   * Filter(FlatMap(Entity(Person, Person(...)), Id(p, Person(...)), Map(FlatFilter(BinaryOp(Property(Id(p, Person(...)), age, Visible), >, Int(18))), Id(unused, BE), Id(p, Person(...)))), Id(ccc, Person(...)), BinaryOp(Property(Id(ccc, Person(...)), name, Visible), ==, String(Main St)))
   * -> FlatMap(Entity(Person, Person(...)), Id(p, Person(...)), Filter(Map(FlatFilter(BinaryOp(Property(Id(p, Person(...)), age, Visible), >, Int(18))), Id(unused, BE), Id(p, Person(...))), Id(ccc, Person(...)), BinaryOp(Property(Id(ccc, Person(...)), name, Visible), ==, String(Main St))))
   * ```
   */
  object DetachableFlatMap {
    operator fun <AP : Pattern<XR.Query>, BP : Pattern<XR.Query>> get(x: AP, y: BP) =
      customPattern2M("DetachableFlatMap", x, y) { it: XR.FlatMap ->
        with(it) {
          when {
            body is XR.U.HasHead && body.head is XR.FlatJoin -> null
            body is XR.U.HasHead && body.head is XR.FlatFilter -> null
            else -> Components2M(head, id, body)
          }
        }
      }
  }

  /**
   * FilterPushableMap matches FlatMaps that have FlatJoin or FlatFilter in their body.
   * These are cases where SqlQueryModel has forced nesting, but we can safely push
   * the outer filter into the inner query's WHERE clause.
   *
   * Case 1 - FlatJoin:
   * Pattern: Filter(FlatMap(a, b, Map(FlatJoin(...), c, d)), e, f)
   * Example SQL before: SELECT t.* FROM (SELECT ... FROM p INNER JOIN a ...) AS t WHERE t.city = 'NYC'
   * Reduction: FlatMap(a, b, Filter(Map(FlatJoin(...), c, d), e, f))
   * Example SQL after: SELECT ... FROM p INNER JOIN a ... WHERE ... AND city = 'NYC'
   *
   * Case 2 - FlatFilter:
   * Pattern: Filter(FlatMap(a, b, Map(FlatFilter(g), c, d)), e, f)
   * Example SQL before: SELECT t.* FROM (SELECT ... FROM p WHERE p.age > 18) AS t WHERE t.name = 'Joe'
   * Reduction: FlatMap(a, b, Filter(Map(FlatFilter(g), c, d), e, f))
   * Example SQL after: SELECT ... FROM p WHERE p.age > 18 AND name = 'Joe'
   *
   * IMPORTANT: Similar to DetachableMap, we cannot apply this reduction if:
   * 1. The FlatMap body contains impurities (like rand(), now(), etc.) - pushing filters through
   *    impure operations can change evaluation order and semantics
   * 2. The FlatMap body is a DistinctOn - filter pushing would change the distinct semantics
   * 3. The filter body contains aggregations (like count(), sum(), etc.) - these must remain
   *    as HAVING clauses after GROUP BY, not be pushed into WHERE clauses
   */
  object FilterPushableFlatMap {
    operator fun <AP : Pattern<XR.Query>, BP : Pattern<XR.Query>> get(x: AP, y: BP) =
      customPattern2M("FilterPushableMap", x, y) { it: XR.FlatMap ->
        with(it) {
          when {
            body.hasAggregations() -> null
            body.hasImpurities() -> null
            body is XR.DistinctOn -> null
            ContainsXR.byType<XR.FlatGroupBy>(body) -> null
            // FlatMap(a, b, Map(FlatJoin(...), c, d))
            body is XR.U.HasHead && body.head is XR.FlatJoin -> Components2M(head, id, body)
            // FlatMap(a, b, Map(FlatFilter(g), c, d))
            body is XR.U.HasHead && body.head is XR.FlatFilter -> Components2M(head, id, body)
            else -> null
          }
        }
      }
  }

  operator fun invoke(q: Query): XR.Query? =
    on(q).match(
      // ---------------------------
      // *.filter

      // a.filter(b => c).filter(d => e) =>
      //    a.filter(b => c && e[d := b])
      //
      // Filter(Filter(a, b, c), d, e) =>
      case(Filter[Filter[Is(), Is()], Is()]).thenThis { (a, b, c), d, e ->
        val er = BetaReduction(e, d to b)
        trace("AdHoc-Reducing Filter[Filter] for:$q") andReturn {
          Filter.cs(a, b, XR.BinaryOp(c, OP.And, er.asExpr()))
        }
      },
      // ---------------------------
      // flatMap.*

      // a.flatMap(b => c).map(d => e) =>
      //    a.flatMap(b => c.map(d => e))
      //
      // Map(FlatMap(a, b, c), d, e) =>
      case(Map[FlatMap[Is(), Is()], Is()]).then { (a, b, c), d, e ->
        trace("AdHoc-Reducing Map[FlatMap] for:$q") andReturn {
          FlatMap.csf(a, b, XR.Map.csf(c, d, e)(comp))(compLeft)
        }
      },
      // a.flatMap(b => c).filter(d => e) =>
      //    a.flatMap(b => c.filter(d => e))
      //
      // case Filter(FlatMap(a, b, c), d, e) =>
      case(Filter[DetachableFlatMap[Is(), Is()], Is()]).then { (a, b, c), d, e ->
        trace("AdHoc-Reducing Filter[FlatMap] for:$q") andReturn {
          FlatMap.csf(a, b, Filter.csf(c, d, e)(comp))(compLeft)
        }
      },
      // a.flatMap(b => c with FlatJoin/FlatFilter).filter(d => e) =>
      //    a.flatMap(b => c.filter(d => e))
      //
      // This handles the case where SqlQueryModel forced nesting due to FlatJoin/FlatFilter,
      // but we can safely push the filter down to merge WHERE clauses.
      //
      // FULL TRANSFORMATION CHAIN EXAMPLE (for FlatFilter case):
      //
      // Input:
      //   Filter(FlatMap(Person, p, Map(FlatFilter(p.age > 18), t, t)), u, u.name == "Joe")
      //   SQL: SELECT * FROM (SELECT * FROM Person WHERE age > 18) AS t WHERE t.name = 'Joe'
      //
      // Step 1 - FilterPushableFlatMap (this reduction):
      //   Filter(FlatMap(Person, p, Map(FlatFilter(p.age > 18), t, t)), u, u.name == "Joe")
      //   => FlatMap(Person, p, Filter(Map(FlatFilter(p.age > 18), t, t), u, u.name == "Joe"))
      //
      // Step 2 - ApplyMap (pushes Filter through Map):
      //   Filter(Map(FlatFilter(p.age > 18), t, t), u, u.name == "Joe")
      //   => Map(Filter(FlatFilter(p.age > 18), t, u.name == "Joe"[u := t]), t, t)
      //   => Map(Filter(FlatFilter(p.age > 18), t, t.name == "Joe"), t, t)
      //   (ApplyMap line 136-139: pushes Filter before Map, beta-reducing the filter body)
      //
      // Step 3 - SymbolicReduction (merges Filter with FlatFilter):
      //   Filter(FlatFilter(p.age > 18), t, t.name == "Joe")
      //   => FlatFilter(p.age > 18 AND t.name == "Joe")
      //   (SymbolicReduction line 45-46: merges using head.by _And_ body to preserve order)
      //
      // Final Result:
      //   FlatMap(Person, p, Map(FlatFilter(p.age > 18 AND t.name == "Joe"), t, t))
      //   SQL: SELECT * FROM Person WHERE age > 18 AND name = 'Joe'
      //
      // Note: Steps 2 & 3 happen in subsequent normalization passes (ApplyMap, then SymbolicReduction)
      //
      // case Filter(FlatMap(a, b, c), d, e) where c has FlatJoin/FlatFilter =>
      case(Filter[FilterPushableFlatMap[Is(), Is()], Is()]).then { (a, b, c), d, e ->
        trace("AdHoc-Reducing Filter[FilterPushableMap] for:$q") andReturn {
          FlatMap.csf(a, b, Filter.csf(c, d, e)(comp))(compLeft)
        }
      },

      // SymbolicReduction already does these
      //// a.flatMap(b => c.union(d))
      ////    a.flatMap(b => c).union(a.flatMap(b => d))
      ////
      //// case FlatMap(a, b, Union(c, d)) =>
      //case(FlatMap[Is(), Union[Is(), Is()]]).then { a, b, (c, d) ->
      //  Union.csf(FlatMap.csf(a, b, c)(comp), FlatMap.csf(a, b, d)(comp))(compRight)
      //},
      //// a.flatMap(b => c.unionAll(d))
      ////    a.flatMap(b => c).unionAll(a.flatMap(b => d))
      ////
      //// case FlatMap(a, b, UnionAll(c, d)) =>
      //case(FlatMap[Is(), UnionAll[Is(), Is()]]).then { a, b, (c, d) ->
      //  UnionAll.csf(FlatMap.csf(a, b, c)(comp), FlatMap.csf(a, b, d)(comp))(compRight)
      //}
    )
}

/*
  def unapply(q: Query) =
    q match {

      // ---------------------------
      // *.filter

      // a.filter(b => c).filter(d => e) =>
      //    a.filter(b => c && e[d := b])
      case Filter(Filter(a, b, c), d, e) =>
        val er = BetaReduction(e, d -> b)
        Some(Filter(a, b, BinaryOperation(c, BooleanOperator.`&&`, er)))

      // ---------------------------
      // flatMap.*

      // a.flatMap(b => c).map(d => e) =>
      //    a.flatMap(b => c.map(d => e))
      case Map(FlatMap(a, b, c), d, e) =>
        Some(FlatMap(a, b, Map(c, d, e)))

      // a.flatMap(b => c).filter(d => e) =>
      //    a.flatMap(b => c.filter(d => e))
      case Filter(FlatMap(a, b, c), d, e) =>
        Some(FlatMap(a, b, Filter(c, d, e)))

      // a.flatMap(b => c.union(d))
      //    a.flatMap(b => c).union(a.flatMap(b => d))
      case FlatMap(a, b, Union(c, d)) =>
        Some(Union(FlatMap(a, b, c), FlatMap(a, b, d)))

      // a.flatMap(b => c.unionAll(d))
      //    a.flatMap(b => c).unionAll(a.flatMap(b => d))
      case FlatMap(a, b, UnionAll(c, d)) =>
        Some(UnionAll(FlatMap(a, b, c), FlatMap(a, b, d)))

      case other => None
    }
 */
