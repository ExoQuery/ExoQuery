package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object CrunchFlatJoinsNoPushAliasReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "map on join bundle with impurities then join - all phases enabled - fully pure/XR" to kt(
      """select { val r = from({ select { val c = from(Table(Customer)); val o = join(Table(Order)) { o.customerId == c.customerId }; Row(name = c.name, totalSpend = c.totalSpend, orderId = o.orderId) } }.toQuery.apply().map { rr -> Row(name = rr.name, totalSpend = rr.totalSpend, orderId = rr.orderId + free("currentVersion()").asPure()) }); val io = join(Table(OrderItem)) { io.orderId == r.orderId }; groupBy(r.name); Pair(first = r.name, second = sum_GC(r.totalSpend)) }"""
    ),
    "map on join bundle with impurities then join - all phases enabled - fully pure" to cr(
      """SELECT c.name AS first, sum(c.totalSpend) AS second FROM Customer c INNER JOIN "Order" o ON o.customerId = c.customerId INNER JOIN OrderItem io ON io.orderId = (o.orderId + currentVersion()) GROUP BY c.name"""
    ),
    "map on joined table with impurities - all phases enabled - fully pure/XR" to kt(
      """select { val r = from({ select { val c = from(Table(Customer)); val o = join(Table(Order)) { o.customerId == c.customerId }; Row(name = c.name, totalSpend = c.totalSpend, orderId = o.orderId) } }.toQuery.apply()); val io = join(Table(OrderItem).map { it -> OrderItemMapped(orderItemId = it.orderItemId, orderId = it.orderId + free("currentVersion()").asPure(), qty = it.qty, unitPrice = it.unitPrice) }) { { r, ioo -> ioo.orderId == r.orderId }.apply(r, io) }; groupBy(r.name); Pair(first = r.name, second = sum_GC(r.totalSpend)) }"""
    ),
    "map on joined table with impurities - all phases enabled - fully pure" to cr(
      """SELECT c.name AS first, sum(c.totalSpend) AS second FROM Customer c INNER JOIN "Order" o ON o.customerId = c.customerId INNER JOIN OrderItem io ON io.orderId = o.orderId AND io.orderItemId, io.orderId + currentVersion(), io.qty, io.unitPrice GROUP BY c.name"""
    ),
  )
}
