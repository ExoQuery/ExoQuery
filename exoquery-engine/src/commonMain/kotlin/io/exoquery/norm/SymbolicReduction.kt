package io.exoquery.norm

import io.exoquery.util.TraceConfig
import io.exoquery.xr.BetaReduction
import io.exoquery.xr.ContainsXR
import io.exoquery.xr.XR
import io.exoquery.xr._And_
import io.exoquery.xr.contains
import io.exoquery.xr.copy.*
import io.exoquery.xr.isDetachablePure

/**
 * This stage represents Normalization Stage1: Symbolic Reduction in Philip
 * Wadler's Paper "A Practical Theory of Language Integrated Query", given in
 * Figure 11.
 * https://homepages.inf.ed.ac.uk/slindley/papers/practical-theory-of-linq.pdf
 *
 * It represents foundational normalizations done to sequences that represents
 * queries. In Wadler's paper, he characterizes them as `for x in P ...``
 * whereas in Quill they are characterized as list comprehensions i.e.
 * `P.flatMap(x => ...)`.
 *
 * NOTE: Leaving Quill commneted-out code equivalents here for now for reference
 */
class SymbolicReduction(val traceConfig: TraceConfig, val queryContainsFlatUnits: Boolean) {
  fun XR.FlatMap.hasTailPositionFlatJoin(): Boolean =
    body is XR.U.HasHead && body.head is XR.FlatJoin

  fun XR.containsNonFilterFlatUnit(): Boolean =
    ContainsXR(this) { it is XR.U.FlatUnit && it !is XR.FlatFilter }

  operator fun invoke(q: XR.Query): XR.Query? =
    with(q) {
      when {

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
         */
        this is XR.Filter && head is XR.U.FlatUnit && head is XR.FlatFilter -> { // TODO && head is XR.U.FlatUnit &&  is redundant because FlatFilter extends FlatUnit
          XR.FlatFilter(head.by _And_ body)
        }

        /*
         * The Filter(Map(FlatJoin(...))) case
         *
         * This is my own transformation as opposed to being from Wadler's paper. It addresses a specific scenario that arises
         * in ExoQuery when dealing Filters around Maps that have FlatJoins inside them.
         *
         * FULL CONTEXT:
         *   FlatMap(
         *     Entity(Person, ...),
         *     p,
         *     Filter(                           <-- This nested structure gets transformed
         *       Map(
         *         FlatJoin(Entity(Address, ...), a, joinCondition),
         *         a,
         *         body
         *       ),
         *       pair,
         *       filterCondition
         *     )
         *   )
         *
         * WHY THIS MATTERS:
         * The outer FlatMap over Entity(Person) cannot flatten into a single SQL query when its
         * body contains a Filter-Map-FlatJoin chain. By restructuring the nested part into
         * FlatMap-FlatFilter, the entire query can collapse into one SQL statement with proper
         * JOIN and WHERE clauses instead of nested subqueries and what will result
         * is a invalid query that will have a `FROM INNER JOIN ...` pattern similar to
         * what happens in the SqlQueryModel Filter(FLatMap(FlatJoin)) case.
         *
         * TRANSFORMATION:
         *
         *   BEFORE (the nested part):
         *     Filter(
         *       Map(FlatJoin(Entity(Address, ...), a, joinCondition), a, body),
         *       pair,
         *       filterCondition  // e.g., pair.first.name == "Joe"
         *     )
         *
         *   AFTER (the nested part):
         *     FlatMap(
         *       FlatJoin(Entity(Address, ...), a, joinCondition),
         *       a,
         *       Map(FlatFilter(reducedCondition), a, body)  // e.g., p.name == "Joe"
         *     )
         *
         * The transformation beta-reduces the filter condition, replacing the intermediate tuple
         * binding (pair) with direct references to the actual entities (p, a).
         */
        this is XR.Filter && head is XR.Map && head.head is XR.FlatJoin -> {
          val flatJoin = head.head        // FlatJoin(Inner, Entity(AddressCrs...), Id(a), onCondition)
          val mapId = head.id             // Id(a, AddressCrs(...))
          val mapBody = head.body         // Product(Tuple, List(...))
          val filterId = id               // Id(pair, Pair(...))
          val filterBody = body           // BinaryOp(Property(Property(Id(pair...), first), name), ==, String(JoeOuter))

          // Beta-reduce the filter condition: replace filterId with mapBody
          // This transforms: pair.first.name == "JoeOuter" into: Product(...).first.name == "JoeOuter"
          // which will further reduce to the actual field reference
          val reducedFilterBody = BetaReduction(filterBody, filterId to mapBody).asExpr()

          // Create the new structure:
          // FlatMap(FlatJoin(...), mapId, Map(FlatFilter(reducedFilterBody), mapId, mapBody))
          XR.FlatMap(
            flatJoin,
            mapId,
            XR.Map(
              XR.FlatFilter(reducedFilterBody),
              mapId,
              mapBody
            )
          )
        }

        /*
         * Represents if-for in Figure 11.
         * This transformation is particularity difficult to understand so I have added examples with several layers of granularity.
         * It's basic form goes like this:
         *
         * bottles.filter(bottle => isMerlot).flatMap(merlotBottle => {withCheese})
         *     bottles.flatMap(merlotBottle => {withCheese}.filter(_ => isMerlot[bottle := merlotBottle]))
         *
         * For example:
         *   The AST block withCheese is cheeses.filter(cheese => cheese.pairsWith == merlotBottle)
         *   The AST block isMerlot is bottle.isMerlot
         *
         * bottles.filter(bottle => bottle.isMerlot).flatMap(merlotBottle => {cheeses.filter(cheese => cheese.pairsWith == merlotBottle)} )
         *     bottles.flatMap(merlotBottle => cheeses.filter({bottle.isMerlot}[bottle := merlotBottle]).filter(cheese => cheese.pairsWith == merlotBottle)}
         * which is:
         *     bottles.flatMap(merlotBottle => cheeses.filter({doesnt-matter} => merlotBottle.isMerlot).filter(cheese => cheese.pairsWith == merlotBottle)
         *
         * a.filter(b => c).flatMap(d => e.$) =>
         *     a.flatMap(d => e.filter(_ => c[b := d]).$)
         *
         * case FlatMap(Filter(a, b, c), d, e: Query)
         *
         * Note: When ApplyMap pushes Filter through Map, it produces Filter(FlatFilter(original), _, new).
         * We use (head.by _And_ body) to preserve chronological order: FlatFilter(original AND new)
         */
        this is XR.FlatMap && head is XR.Filter && !queryContainsFlatUnits -> {  // TODO for the sake of performance can do containsNonFilterFlatUnit on the entire query first
          val (a, b, c) = Triple(head.head, head.id, head.body)
          val (d, e) = Pair(id, body)
          val cr = BetaReduction(c, b to d).asExpr()
          val er = with(head) {
            AttachToEntity({ q, id -> Filter.cs(q, id, cr) })(e)
          }
          FlatMap.cs(a, d, er)
        }

        /*
         * This transformation does not have an analogue in Wadler's paper, it represents the fundamental nature of the Monadic 'bind' function
         * that A.flatMap(a => B).flatMap(b => C) is isomorphic to A.flatMap(a => B.flatMap(b => C)).
         *
         * a.flatMap(b => c).flatMap(d => e) =>
         *     a.flatMap(b => c.flatMap(d => e))
         *
         * case FlatMap(FlatMap(a, b, c), d, e) =>
         *     Some(FlatMap(a, b, FlatMap(c, d, e)))
         *
         * IMPORTANT CAVEAT - Map(FlatJoin, projection) Problem and Solution:
         *
         * This transformation can create a problematic intermediate structure: FlatMap(head, id, Map(FlatJoin, projection)).
         * The Map(FlatJoin, projection) construct would generate invalid SQL like:
         * "SELECT projection FROM INNER JOIN table ..." (missing the left-hand table for the join).
         *
         * However, we rely on ApplyMap to fix this problem AFTER this transformation runs.
         * ApplyMap has two cases that handle FlatMap(Map(FlatJoin, projection), ...):
         *
         * 1. case(XR.FlatMap[DetachableMap[Is(), Is()], Is()]) - handles pure projections
         *    Transforms: FlatMap(Map(FlatJoin(table), id, projection), bindId, body)
         *    To: FlatMap(FlatJoin(table), id, BetaReduction(body, bindId -> projection))
         *
         * 2. case(XR.FlatMap[XR.Map[Is<XR.FlatJoin>(), Is()], Is()]) - handles impure projections
         *    Same transformation but doesn't check for purity, because Map(FlatJoin, projection)
         *    should NEVER exist regardless of projection purity.
         *
         * FULL EXAMPLE:
         *
         * Input (before this transformation):
         *   FlatMap(
         *     FlatMap(Table<Person>, p, Map(FlatJoin(Table<Address>), a, Tuple(p, a))),
         *     kv,
         *     Map(FlatJoin(Table<Robot>), r, Triple(kv.first.name, kv.second.city, r.name))
         *   )
         *
         * Step 1 - This SymbolicReduction transformation flattens FlatMap(FlatMap):
         *   FlatMap(Table<Person>, p,
         *     FlatMap(
         *       Map(FlatJoin(Table<Address>), a, Tuple(p, a)),  // <- PROBLEMATIC: Map(FlatJoin)!
         *       kv,
         *       Map(FlatJoin(Table<Robot>), r, Triple(...))     // <- PROBLEMATIC: Map(FlatJoin)!
         *     )
         *   )
         *
         *   At this point, if SqlQueryModel tried to process Map(FlatJoin(...)), it would generate:
         *   "SELECT ... FROM INNER JOIN Address ..." (invalid SQL - no left-hand table)
         *
         * Step 2 - ApplyMap's FlatMap(Map(FlatJoin)) case fixes the outer Map(FlatJoin):
         *   FlatMap(Table<Person>, p,
         *     FlatMap(
         *       FlatJoin(Table<Address>), a,                    // <- FlatJoin extracted from Map!
         *       BetaReduction(
         *         Map(FlatJoin(Table<Robot>), r, Triple(...)),
         *         kv -> Tuple(p, a)
         *       )
         *     )
         *   )
         *
         *   Beta reduction substitutes kv with Tuple(p, a) in the body, transforming:
         *   Triple(kv.first.name, kv.second.city, r.name)
         *   to: Triple(p.name, a.city, r.name)
         *
         * Step 3 - ApplyMap runs again on the nested FlatMap and fixes the inner Map(FlatJoin):
         *   FlatMap(Table<Person>, p,
         *     FlatMap(
         *       FlatJoin(Table<Address>), a,
         *       FlatMap(
         *         FlatJoin(Table<Robot>), r,                    // <- FlatJoin extracted from Map!
         *         Map(Triple(p.name, a.city, r.name), _, _)
         *       )
         *     )
         *   )
         *
         * Final Result - Valid structure ready for SqlQueryModel:
         *   All Map(FlatJoin) constructs have been eliminated by ApplyMap.
         *   SqlQueryModel can now generate valid SQL:
         *   "SELECT p.name, a.city, r.name FROM Person p INNER JOIN Address a ... INNER JOIN Robot r ..."
         *
         * This is why we can safely apply this FlatMap(FlatMap) transformation without restrictions -
         * we're relying on ApplyMap to clean up any Map(FlatJoin) problems that arise.
         */
        this is XR.FlatMap && head is XR.FlatMap -> {
          val (a, b, c) = Triple(head.head, head.id, head.body)
          val (d, e) = Pair(id, body)
          FlatMap.cs(a, b, FlatMap.cs(c, d, e))
        }

// Scala:
//      case FlatMap(FlatMap(a, b, c), d, e) =>
//        Some(FlatMap(a, b, FlatMap(c, d, e)))

        // Represents for@ in Figure 11
        //
        // a.union(b).flatMap(c => d)
        //      a.flatMap(c => d).union(b.flatMap(c => d))
        //
        // case FlatMap(Union(a, b), c, d) =>
        //     Some(Union(FlatMap(a, c, d), FlatMap(b, c, d)))
        this is XR.FlatMap && head is XR.Union && !body.hasImpurities() -> {
          val (a, b) = Pair(head.a, head.b)
          val (c, d) = Pair(id, body)
          with(head) {
            Union.cs(FlatMap.cs(a, c, d), FlatMap.cs(b, c, d))
          }
        }

// Scala
//      case FlatMap(Union(a, b), c, d) =>
//        Some(Union(FlatMap(a, c, d), FlatMap(b, c, d)))

        // Represents for@ in Figure 11 (Wadler does not distinguish between Union and UnionAll)
        //
        // a.unionAll(b).flatMap(c => d)
        //      a.flatMap(c => d).unionAll(b.flatMap(c => d))
        this is XR.FlatMap && head is XR.UnionAll && !body.hasImpurities() -> {
          val (a, b) = Pair(head.a, head.b)
          val (c, d) = Pair(id, body)
          with(head) {
            UnionAll.cs(FlatMap.cs(a, c, d), FlatMap.cs(b, c, d))
          }
        }


        else -> null
      }
    }
}
