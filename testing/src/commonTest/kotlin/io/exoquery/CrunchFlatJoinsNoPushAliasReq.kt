@file:PhasesDisabled(DisableablePhase.PushAlias::class, DisableablePhase.Dealias::class)

package io.exoquery

import io.exoquery.annotation.PhasesDisabled
import io.exoquery.util.DisableablePhase

/**
 * TESTING BETA REDUCTION IN CRUNCHFLATJOINS WITHOUT PUSHALIAS
 *
 * This test suite validates the BetaReduction logic in CrunchFlatJoins.kt that handles
 * Map(FlatJoin) structures when PushAlias is disabled.
 *
 * CONTEXT:
 * In CrunchFlatJoins.kt, there is a pattern that applies BetaReduction to join filters:
 * ```kotlin
 * case(XR.FlatJoin[DetachableMap[Is(), Is()], Is()]).then { (source, fid, filter), jid, on ->
 *   XR.FlatJoin.csf(source, jid, on _And_ BetaReduction(filter, fid to jid).asExpr())(comp)
 * }
 * ```
 *
 * Under normal operation with PushAlias enabled, the outer alias from `join(Table<OrderItem>())`
 * would be pushed into the subsequent `.map { it -> OrderItemMapped(...) }` transformation,
 * rendering the BetaReduction unnecessary as the aliases are already aligned.
 *
 * However, when we forcibly disable PushAlias, the outer join alias is NOT pushed into the
 * map body, creating a Map(FlatJoin) structure where the map's lambda parameter (fid) differs
 * from the join's identifier (jid). The BetaReduction is then necessary to substitute the
 * lambda parameter with the join identifier in the filter expression.
 *
 * WHY PUSHALIAS IS DISABLED:
 * PushAlias (when enabled) would normalize the structure by pushing the join alias into the
 * map body, preventing us from testing the BetaReduction logic. By disabling PushAlias, we
 * preserve the original Map(FlatJoin) structure that requires BetaReduction to properly align
 * the identifiers in join conditions.
 *
 * This test ensures that even without PushAlias optimization, CrunchFlatJoins can still
 * correctly handle Map(FlatJoin) structures through BetaReduction.
 */
class CrunchFlatJoinsNoPushAliasReq: GoldenSpecDynamic(CrunchFlatJoinsNoPushAliasReqGoldenDynamic, Mode.ExoGoldenTest(), {

  data class Customer(val customerId: Int, val name: String, val totalSpend: Double)

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

  data class OrderItemMapped(
    val orderItemId: Int,
    val orderId: Int,
    val qty: Int,
    val unitPrice: Double
  )

  data class Row(val name: String, val totalSpend: Double, val orderId: Int)

  // Bundle the join in a fragment
  @SqlFragment
  fun customerOrderItems(): SqlQuery<Row> = sql.select {
    val c = from(Table<Customer>())
    val o = join(Table<Order>()) { o -> o.customerId == c.customerId }
    Row(c.name, c.totalSpend, o.orderId)
  }

  "map on join bundle with impurities then join - all phases enabled - fully pure" {
    val q = sql.select {
      val r = from(customerOrderItems().map { rr -> Row(rr.name, rr.totalSpend, rr.orderId + free("currentVersion()").asPure<Int>()) })
      val io = join(Table<OrderItem>()) { io -> io.orderId == r.orderId }
      groupBy(r.name)
      Pair(r.name, sum(r.totalSpend))
    }.dynamic()

    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }

  /**
   * TEST: Map on joined table with pure impurities (PushAlias and Dealias disabled)
   *
   * This test validates that CrunchFlatJoins can handle Map(FlatJoin) structures through
   * BetaReduction when PushAlias is disabled. The join condition `io.orderId == r.orderId`
   * references the map's lambda parameter (io), which needs to be beta-reduced to align
   * with the actual FlatJoin identifier.
   *
   * Structure:
   * - join(Table<OrderItem>().map { it -> OrderItemMapped(...) }) { io -> io.orderId == r.orderId }
   *   creates: FlatJoin(Map(Entity(OrderItem), it, OrderItemMapped(...)), io, io.orderId == r.orderId)
   *
   * Without PushAlias and Dealias, the map's lambda parameter (it) is not replaced with the outer join
   * alias (io), so BetaReduction must substitute:
   * - io.orderId in the join condition with the actual map body references
   *
   * The BetaReduction pattern in CrunchFlatJoins handles this:
   * ```kotlin
   * case(XR.FlatJoin[DetachableMap[Is(), Is()], Is()]).then { (source, fid, filter), jid, on ->
   *   XR.FlatJoin.csf(source, jid, on _And_ BetaReduction(filter, fid to jid).asExpr())(comp)
   * }
   * ```
   *
   * This ensures that even without PushAlias optimization, the join condition is correctly
   * constructed with proper identifier alignment.
   */
  "map on joined table with impurities - all phases enabled - fully pure" {
    @SqlFragment
    fun joinCond(r: Row, ioo: OrderItemMapped): SqlExpression<Boolean> = sql.expression { ioo.orderId == r.orderId }

    // Join with map on the joined table itself
    val q = sql.select {
      val r = from(customerOrderItems())
      val io = join(Table<OrderItem>().map { it -> OrderItemMapped(it.orderItemId, it.orderId + free("currentVersion()").asPure<Int>(), it.qty, it.unitPrice) }) { io -> joinCond(r, io).use }
      groupBy(r.name)
      Pair(r.name, sum(r.totalSpend))
    }.dynamic()

    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }
})
