package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object FlatJoinLastResortReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "filtered join bundle with aggregation/XR" to kt(
      "select { val r = from({ base -> base.filter { it -> it.o.status == PAID } }.toQuery.apply({ select { val c = from(Table(Customer)); val o = join(Table(Order)) { o.customerId == c.customerId }; val oi = join(Table(OrderItem)) { oi.orderId == o.orderId }; Row(c = c, o = o, oi = oi) } }.toQuery.apply())); val gross = /*ASI*/ sum_GC(r.oi.qty * r.oi.unitPrice); val orders = /*ASI*/ countDistinct_GC(r.o.orderId); groupBy(r.c.customerId); having(gross > 0.0); Agg(customerId = r.c.customerId, ordersCount = orders, gross = gross) }"
    ),
    "filtered join bundle with aggregation" to cr(
      """SELECT r.c_customerId AS customerId, count(DISTINCT r.o_orderId) AS ordersCount, sum(r.oi_qty * r.oi_unitPrice) AS gross FROM (SELECT c.customerId AS c_customerId, c.name AS c_name, o.orderId AS o_orderId, o.customerId AS o_customerId, o.status AS o_status, oi.orderItemId AS oi_orderItemId, oi.orderId AS oi_orderId, oi.qty AS oi_qty, oi.unitPrice AS oi_unitPrice FROM Customer c INNER JOIN "Order" o ON o.customerId = c.customerId INNER JOIN OrderItem oi ON oi.orderId = o.orderId WHERE o.status = 'PAID') AS r GROUP BY r.c_customerId HAVING sum(r.oi_qty * r.oi_unitPrice) > 0.0"""
    ),
  )
}
