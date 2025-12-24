package io.exoquery

import io.exoquery.annotation.SqlFragment

class QueryFilterFlatteningReq: GoldenSpecDynamic(QueryFilterFlatteningReqGoldenDynamic, Mode.ExoGoldenTest(), {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val id: Int, val personId: Int, val city: String)

  "can reduce" - {
    "select-clause->triple + where with filter" {
      val queryA = sql.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.personId == p.id }
        where { p.name == "Joe" }
        Triple(p.name, p.age, a.city)
      }
      val queryB = sql { queryA.filter { t -> t.third == "NewYork" } }.dynamic()
      shouldBeGolden(queryB.xr, "XR")
      shouldBeGolden(queryB.build<PostgresDialect>())
    }

    "select-clause->value + where with filter" {
      val queryA = sql.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.personId == p.id }
        where { p.name == "Joe" }
        p.name
      }
      val queryB = sql { queryA.filter { t -> t == "JoeJoe" } }
      shouldBeGolden(queryB.xr, "XR")
      shouldBeGolden(queryB.build<PostgresDialect>())
    }

    "select stuff, (select-clause->value + where with filter(___ + stuff <pure>))" {
      data class Stuff(val extra: String)

      val queryA = sql.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.personId == p.id }
        where { p.name == "Joe" }
        p.name
      }
      val queryB = sql.select {
        val stuff = from(Table<Stuff>())
        val innerQuery = queryA.filter { t -> t == "JoeJoe" && stuff.extra == "ext" }
        innerQuery.value()
      }
      shouldBeGolden(queryB.xr, "XR")
      shouldBeGolden(queryB.build<PostgresDialect>())
    }
  }

  "cannot reduce" - {
    "anything that contains impurities e.g. impure inlines" {
      data class Stuff(val extra: String)

      val queryA = sql.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.personId == p.id }
        where { p.name == "Joe" }
        p.name to free("rand()")<Int>()
      }
      val queryB = sql.select {
        val stuff = from(Table<Stuff>())
        val innerQuery = queryA.filter { t -> t.first == "JoeJoe" && stuff.extra == "ext" }
        innerQuery.value()
      }.dynamic()
      shouldBeGolden(queryB.xr, "XR")
      shouldBeGolden(queryB.build<PostgresDialect>())
    }
  }

  "composite type fragments with composeFrom" - {
    "should flatten fragment returning composite type when extended with composeFrom" {
      data class Customer(val id: Int, val name: String, val email: String, val tier: String)
      data class Order(val id: Int, val customerId: Int, val orderDate: String, val status: String, val totalAmount: Double)
      data class OrderItem(val id: Int, val orderId: Int, val productId: Int, val quantity: Int, val unitPrice: Double)

      // Composite type bundling common joins
      data class CustomerOrder(val c: Customer, val o: Order)

      // Base fragment returning composite
      @SqlFragment
      fun activeCustomerOrders(): SqlQuery<CustomerOrder> = sql.select {
        val c = from(Table<Customer>())
        val o = join(Table<Order>()) { ord -> ord.customerId == c.id }
        where { o.status == "pending" }
        CustomerOrder(c, o)
      }

      // Extension fragment
      @SqlFragment
      fun Order.items() = sql {
        composeFrom.joinLeft(Table<OrderItem>()) { oi -> oi.orderId == this@items.id }
      }

      data class Result(val customerName: String, val orderId: Int, val totalItems: Int)

      // Query that extends the base with composeFrom
      val query = sql.select {
        val row = from(activeCustomerOrders())
        val oi = from(row.o.items())
        groupBy(row.c.id, row.c.name, row.o.id)
        Result(row.c.name, row.o.id, free("COALESCE(SUM(${oi?.quantity}), 0)").asPure<Int>())
      }

      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.build<PostgresDialect>())
    }

    "should flatten fragment with where clause when extended with composeFrom that also has where" {
      data class Customer(val id: Int, val name: String, val email: String, val tier: String)
      data class Order(val id: Int, val customerId: Int, val orderDate: String, val status: String, val totalAmount: Double)
      data class OrderItem(val id: Int, val orderId: Int, val productId: Int, val quantity: Int, val unitPrice: Double)

      // Composite type bundling common joins
      data class CustomerOrder(val c: Customer, val o: Order)

      // Base fragment returning composite with WHERE clause
      @SqlFragment
      fun activeCustomerOrders(): SqlQuery<CustomerOrder> = sql.select {
        val c = from(Table<Customer>())
        val o = join(Table<Order>()) { ord -> ord.customerId == c.id }
        where { o.status == "pending" }
        CustomerOrder(c, o)
      }

      // Extension fragment
      @SqlFragment
      fun Order.items() = sql {
        composeFrom.joinLeft(Table<OrderItem>()) { oi -> oi.orderId == this@items.id }
      }

      data class Result(val customerName: String, val orderId: Int, val totalItems: Int)

      // Query that extends the base with composeFrom AND adds outer WHERE clause
      val query = sql.select {
        val row = from(activeCustomerOrders())
        val oi = from(row.o.items())
        where { row.c.tier == "premium" }
        groupBy(row.c.id, row.c.name, row.o.id)
        Result(row.c.name, row.o.id, free("COALESCE(SUM(${oi?.quantity}), 0)").asPure<Int>())
      }

      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.build<PostgresDialect>())
    }
  }
})
