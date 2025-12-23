package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Person

class FlatUnitBugReq: GoldenSpecDynamic(FlatUnitBugReqGoldenDynamic, Mode.ExoGoldenTest(), {
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
  "flatUnitBug" {
    val q = sql.select {
      val c = from(BCustomers.filter { it.status == "active" && it.region == "US" })
      val o = join(BOrders) { it.customerId == c.id }
      val i = join(BOrderItems) { it.orderId == o.id }
      val p = join(BProducts) { it.id == i.productId }
      val cat = join(BCategories) { it.id == p.categoryId }
      where { (cat.name == "Electronics") || (cat.name == "Books") }
      c.id to c.name to (i.quantity to i.unitPrice)
    }
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "flatUnitBug - groupBy" {
    val q = sql.select {
      val c = from(BCustomers.filter { it.status == "active" && it.region == "US" })
      val o = join(BOrders) { it.customerId == c.id }
      val i = join(BOrderItems) { it.orderId == o.id }
      val p = join(BProducts) { it.id == i.productId }
      val cat = join(BCategories) { it.id == p.categoryId }
      groupBy(c.id)
      c.id to c.name to (i.quantity to i.unitPrice)
    }
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "flatUnitBug - firstTwoFiltered" {
    val q = sql.select {
      val c = from(BCustomers.filter { it.status == "active" && it.region == "US" })
      val o = join(BOrders.filter { it.orderNumber == 123 }) { it.customerId == c.id }
      val i = join(BOrderItems) { it.orderId == o.id }
      val p = join(BProducts) { it.id == i.productId }
      val cat = join(BCategories) { it.id == p.categoryId }
      where { (cat.name == "Electronics") || (cat.name == "Books") }
      c.id to c.name to (i.quantity to i.unitPrice)
    }
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }
})
