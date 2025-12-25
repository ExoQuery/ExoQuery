@file:PhasesDisabled(DisableablePhase.CrunchFlatJoins::class)
@file:TracesEnabled(TraceType.SqlNormalizations::class, TraceType.Normalizations::class, TraceType.SqlQueryConstruct::class, TraceType.Standard::class)
package io.exoquery

import io.exoquery.*
import io.exoquery.annotation.PhasesDisabled
import io.exoquery.annotation.TracesEnabled
import io.exoquery.util.DisableablePhase
import io.exoquery.util.TraceType
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


object ChatGptIssueRepro {

  @Serializable
  data class Customer(@SerialName("customer_id") val customerId: Int, val name: String)

  @Serializable
  data class Order(
    @SerialName("order_id") val orderId: Int,
    @SerialName("customer_id") val customerId: Int,
    val status: String
  )

  @Serializable
  data class OrderItem(
    @SerialName("order_item_id") val orderItemId: Int,
    @SerialName("order_id") val orderId: Int,
    val qty: Int,
    @SerialName("unit_price") val unitPrice: Double
  )

  @Serializable
  data class Row(val c: Customer, val o: Order, val oi: OrderItem)

  // 1) Bundle the 3-table join in a fragment
  @SqlFragment
  fun customerOrderItems(): SqlQuery<Row> = sql.select {
    val c = from(Table<Customer>())
    val o = join(Table<Order>()) { o -> o.customerId == c.customerId }
    val oi = join(Table<OrderItem>()) { oi -> oi.orderId == o.orderId }
    Row(c, o, oi)
  }

  // 2) Filter on Order AFTER the join bundle (this is key)
  @SqlFragment
  fun paidOnly(base: SqlQuery<Row>): SqlQuery<Row> = sql {
    base.filter { it.o.status == "PAID" }
  }

  @Serializable
  data class Agg(val customerId: Int, val ordersCount: Int, val gross: Double)

  // 3) Aggregate using both Order + OrderItem
  val q = sql.select {
    val r = from(paidOnly(customerOrderItems()))
    val gross = sum(r.oi.qty * r.oi.unitPrice)
    val orders = countDistinct(r.o.orderId)

    groupBy(r.c.customerId)
    having { gross > 0.0 }

    Agg(r.c.customerId, orders, gross)
  }.dynamic()
}


// TODO need to test this with CruchFlatJoins disabled to test the FlatJoinLastResort
// TODO add the test WITHOUT last-resort in regular QueryReq
fun main() {
  println(ChatGptIssueRepro.q.buildPrettyFor.Sqlite().value)
}
