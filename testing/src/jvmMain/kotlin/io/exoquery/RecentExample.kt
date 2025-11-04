@file:io.exoquery.annotation.TracesEnabled(TraceType.SqlQueryConstruct::class, TraceType.SqlNormalizations::class, TraceType.Normalizations::class)

package io.exoquery

import io.exoquery.util.TraceType
import kotlinx.serialization.Serializable


data class BCustomer(val id: Int, val name: String, val status: String, val region: String)
data class BCategory(val id: Int, val name: String)
data class BProduct(val id: Int, val categoryId: Int, val name: String, val price: Double)
data class BOrder(val id: Int, val customerId: Int, val orderNumber: Int, val createdAt: String)
data class BOrderItem(val id: Int, val orderId: Int, val productId: Int, val quantity: Int, val unitPrice: Double)
data class BPayment(val id: Int, val orderId: Int, val amount: Double, val paidAt: String, val method: String)
data class BShipment(val id: Int, val orderId: Int, val shippedAt: String?, val carrier: String?, val tracking: String?)

val BCustomers = sql { Table<BCustomer>() }
val BCategories = sql { Table<BCategory>() }
val BProducts = sql { Table<BProduct>() }
val BOrders = sql { Table<BOrder>() }
val BOrderItems = sql { Table<BOrderItem>() }

val q = sql.select {
  val c = from(BCustomers.filter { it.status == "active" && it.region == "US" })
  val o = join(BOrders) { it.customerId == c.id }
  val i = join(BOrderItems) { it.orderId == o.id }
  val p = join(BProducts) { it.id == i.productId }
  val cat = join(BCategories) { it.id == p.categoryId }
  //where { (cat.name == "Electronics") || (cat.name == "Books") }
  groupBy(c.id)

  // c.id, c.name, i.quantity, i.unitPrice
  c.id to c.name to (i.quantity to i.unitPrice)
}

// Build the complex query; we will execute via plain JDBC to avoid controller wiring in this sample
private fun buildExoSql(): String {
  val build = q.buildPrettyFor.Postgres().value
  return build
}

fun main() {
  val sqlText = buildExoSql()
  println(sqlText)
}
