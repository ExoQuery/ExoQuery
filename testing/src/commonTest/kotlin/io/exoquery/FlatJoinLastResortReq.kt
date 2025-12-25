@file:PhasesDisabled(DisableablePhase.SymbolicReduction::class)

package io.exoquery

import io.exoquery.annotation.PhasesDisabled
import io.exoquery.util.DisableablePhase

/**
 * LAST-RESORT FLATJOIN PROCESSING TESTS
 *
 * This test suite specifically validates SqlQueryModel's last-resort handling of problematic
 * FlatJoin structures when earlier normalization phases (most notably CrunchFlatJoins) are
 * disabled or unable to fix them.
 *
 * CONTEXT:
 * Under normal operation, the CrunchFlatJoins phase handles problematic FlatJoin patterns:
 * - Map(FlatJoin(...)) - Map wrapping a FlatJoin which would generate invalid "FROM INNER JOIN"
 * - Filter(FlatJoin(...)) - Filter wrapping a FlatJoin which needs special handling
 *
 * However, when CrunchFlatJoins is disabled or certain edge cases occur, SqlQueryModel must
 * act as a "last resort" to handle these structures. This test suite validates that fallback.
 *
 * KEY SQLQUERYMODEL LAST-RESORT LOGIC (SqlQueryModel.kt:372-380):
 * ```kotlin
 * this is XR.FlatMap ->
 *   val headProcessed =
 *     if (head.isSomeKindOfFlatJoin()) head.processSomeKindOfFlatJoin() else head
 *   //    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *   //    This is the last-resort check that handles Filter(FlatJoin) or bare FlatJoin
 *
 *   val source = sourceSpecific(headProcessed, id.name) ?: QueryContext(invoke(headProcessed), id.name)
 *   val (nestedContexts, finalFlatMapBody) = flattenContexts(body)
 *   (listOf(Layer.Context(source)) + nestedContexts to finalFlatMapBody)
 * ```
 *
 * WHERE isSomeKindOfFlatJoin() IS DEFINED (LastResortFlatJoin.kt):
 * ```kotlin
 * fun XR.Query.isFilteredFlatJoin() =
 *   this is XR.Filter && this.head is XR.FlatJoin
 *
 * fun XR.Query.isSomeKindOfFlatJoin() =
 *   this is XR.FlatJoin || this.isFilteredFlatJoin()
 * ```
 *
 * WHY SYMBOLICREDUCTION IS DISABLED:
 * SymbolicReduction (when enabled) transforms the structure in ways that prevent the last-resort
 * logic from working correctly. For example:
 * - BEFORE: FlatMap(Entity(Person), p, Filter(FlatJoin(Address), a, condition))
 * - AFTER:  FlatMap(Entity(Person), p, FlatMap(FlatJoin(Address), a, Map(FlatFilter(...))))
 *
 * The transformation changes Filter(FlatJoin) into FlatMap(FlatJoin, Map(FlatFilter)) which:
 * 1. No longer matches isSomeKindOfFlatJoin() because the body is now FlatMap, not Filter(FlatJoin)
 * 2. Creates Map(Filter(FlatFilter)) structures that SqlQueryModel cannot handle
 *
 * By disabling SymbolicReduction, we preserve the original Filter(FlatJoin) structure that
 * the last-resort logic is designed to handle.
 *
 * REGULAR TESTS (with all phases enabled):
 * See QueryReq.kt for the same test cases running with full normalization pipeline including
 * CrunchFlatJoins. Those tests validate the "happy path" where CrunchFlatJoins fixes problems
 * before they reach SqlQueryModel.
 */
class FlatJoinLastResortReq: GoldenSpecDynamic(FlatJoinLastResortReqGoldenDynamic, Mode.ExoGoldenTest(), {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val ownerId: Int, val street: String, val city: String)

  /**
   * TEST: Last-resort processing of Filter(FlatJoin) via isSomeKindOfFlatJoin()
   *
   * This test specifically validates the SqlQueryModel.kt:374-375 last-resort logic:
   * ```kotlin
   * val headProcessed =
   *   if (head.isSomeKindOfFlatJoin()) head.processSomeKindOfFlatJoin() else head
   * ```
   *
   * This code path is reached when FlatMap has a head that is either:
   * 1. A bare FlatJoin
   * 2. A Filter(FlatJoin) - which this test exercises
   *
   * REGULAR TEST LOCATION:
   * The same test case WITH CrunchFlatJoins enabled exists in QueryReq.kt. That version
   * validates the "happy path" where CrunchFlatJoins fixes the structure before SqlQueryModel
   * sees it. This test validates that SqlQueryModel can handle it as a fallback.
   *
   * ---
   *
   * BUG REPRODUCTION: SymbolicReduction creates invalid "FROM INNER JOIN" SQL
   *
   * This minimal reproducer demonstrates how SymbolicReduction's transformations can create
   * invalid SQL when combining filtered join bundles with aggregations.
   *
   * WHAT TRIGGERS IT:
   * 1. A fragment that joins Customer -> Order -> OrderItem returning a composite row
   * 2. A second fragment that filters on Order.status AFTER the join bundle
   * 3. An outer query with aggregation over OrderItem (SUM) and COUNT DISTINCT over Order,
   *    with GROUP BY/HAVING
   *
   * THE ROOT CAUSE:
   * SymbolicReduction applies two transformations in sequence:
   *
   * Step 1 - Line 134: FlatMap(Filter) transformation moves Filter inside FlatMap:
   *   BEFORE: FlatMap(Entity(Person), p, Filter(FlatJoin(...), a, city == "Someplace"))
   *   AFTER:  FlatMap(Entity(Person), p, FlatJoin(...).filter(a => city == "Someplace"))
   *
   * Step 2 - Line 84: Filter(Map(FlatJoin)) transformation creates nested FlatMaps:
   *   BEFORE: Filter(Map(FlatJoin(...), a, body), pair, filterCondition)
   *   AFTER:  FlatMap(FlatJoin(...), a, Map(FlatFilter(reducedCondition), a, body))
   *
   * Combined effect on this query:
   *   Initial structure:
   *     FlatMap(Entity(Person), p, Filter(FlatJoin(Address), a, a.city == "Someplace"))
   *
   *   After Step 1 (line 134):
   *     FlatMap(FlatJoin(Address), a, Filter(Entity(Person), p, ...))
   *     // Filter moved inside but structure is wrong
   *
   *   After Step 2 (line 84):
   *     FlatMap(FlatJoin(...), x, Map(Filter(FlatFilter(...)), ...))
   *     // Creates nested Map(Filter(FlatFilter)) which SqlQueryModel can't handle
   *
   * THE PROBLEM IN SqlQueryModel:
   * When SqlQueryModel's base() function encounters FlatFilter at line 482, it falls through
   * to the else case which calls sourceSpecific(FlatFilter, ...). Since FlatFilter is a
   * FlatUnit, sourceSpecific throws an error at line 931:
   *   "Source of a query cannot be a flat-unit (e.g. where/groupBy/sortedBy)"
   *
   * THE BROKEN SQL (when this passes through):
   * ```sql
   * SELECT c.customer_id, count(DISTINCT o.order_id), sum(oi.qty * oi.unit_price)
   * FROM Customer c
   *   INNER JOIN "Order" o ON o.customer_id = c.customer_id,
   *   (
   *     SELECT oi.order_item_id, oi.order_id, oi.qty, oi.unit_price
   *     FROM INNER JOIN OrderItem oi ON oi.order_id = o.order_id   -- ❌ ILLEGAL!
   *     WHERE o.status = 'PAID'
   *   ) AS oi
   * GROUP BY c.customer_id
   * HAVING sum(oi.qty * oi.unit_price) > 0.0
   * ```
   *
   * Runtime error: "no such table: INNER"
   * Because `FROM INNER JOIN ...` is syntactically invalid.
   *
   * THE FIX:
   * By disabling SymbolicReduction with @PhasesDisabled(DisableablePhase.SymbolicReduction::class),
   * the structure remains as FlatMap(Entity, p, Filter(FlatJoin, ...)) which SqlQueryModel
   * handles correctly via the LastResortFlatJoin logic or the isSomeKindOfFlatJoin() check.
   */
  "filtered join bundle with aggregation" {
    data class Customer(val customerId: Int, val name: String)

    data class Order(
      val orderId: Int,
      val customerId: Int,
      val status: String
    )

    data class OrderItem(
      val orderItemId: Int,
      val orderId: Int,
      val qty: Int,
      val unitPrice: Double
    )

    data class Row(val c: Customer, val o: Order, val oi: OrderItem)

    // 1) Bundle the 3-table join in a fragment
    @SqlFragment
    fun customerOrderItems(): SqlQuery<Row> = sql.select {
      val c = from(Table<Customer>())
      val o = join(Table<Order>()) { o -> o.customerId == c.customerId }
      val oi = join(Table<OrderItem>()) { oi -> oi.orderId == o.orderId }
      Row(c, o, oi)
    }

    // 2) Filter on Order AFTER the join bundle (this is key to triggering the bug)
    @SqlFragment
    fun paidOnly(base: SqlQuery<Row>): SqlQuery<Row> = sql {
      base.filter { it.o.status == "PAID" }
    }

    data class Agg(val customerId: Int, val ordersCount: Int, val gross: Double)

    // 3) Aggregate using both Order + OrderItem
    //    This combination forces the problematic transformation
    val q = sql.select {
      val r = from(paidOnly(customerOrderItems()))
      val gross = sum(r.oi.qty * r.oi.unitPrice)
      val orders = countDistinct(r.o.orderId)

      groupBy(r.c.customerId)
      having { gross > 0.0 }

      Agg(r.c.customerId, orders, gross)
    }.dynamic()

    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }
})
