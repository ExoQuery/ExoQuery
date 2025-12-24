package io.exoquery

import io.exoquery.testdata.Address
import io.exoquery.testdata.Person
import io.exoquery.testdata.Robot

class MonadicMachineryReq: GoldenSpecDynamic(MonadicMachineryReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "sql.expression.(Row)->Table" {
    val joinAddress = sql.expression {
      { p: Person -> composeFrom.join(Table<Address>()) { a -> p.id == a.ownerId } }
    }

    val joinRobot = sql.expression {
      { p: Person -> composeFrom.join(Table<Robot>()) { r -> p.id == r.ownerId } }
    }

    val cap = sql.select {
      val p = from(Table<Person>())
      val a = from(joinAddress.use(p))
      val r = from(joinRobot.use(p))
      Triple(p, a, r)
    }

    shouldBeGolden(cap.xr, "XR")
    shouldBeGolden(cap.build<PostgresDialect>(), "SQL")
  }
  "sql.expression.(@Cap (Row)->Table).use" {
    @SqlFragment
    fun joinAddress(p: Person) = sql.expression {
      composeFrom.join(Table<Address>()) { a -> p.id == a.ownerId }
    }

    @SqlFragment
    fun joinRobot(p: Person) = sql.expression {
      composeFrom.join(Table<Robot>()) { r -> p.id == r.ownerId }
    }

    val cap = sql.select {
      val p = from(Table<Person>())
      val a = from(joinAddress(p).use)
      val r = from(joinRobot(p).use)
      Triple(p, a, r)
    }

    shouldBeGolden(cap.xr, "XR")
    shouldBeGolden(cap.build<PostgresDialect>(), "SQL")
  }
  "sql.(@Cap (Row)->Table).use" {
    @SqlFragment
    fun joinAddress(p: Person) = sql {
      composeFrom.join(Table<Address>()) { a -> p.id == a.ownerId }
    }

    @SqlFragment
    fun joinRobot(p: Person) = sql {
      composeFrom.join(Table<Robot>()) { r -> p.id == r.ownerId }
    }

    val cap = sql.select {
      val p = from(Table<Person>())
      val a = from(joinAddress(p))
      val r = from(joinRobot(p))
      Triple(p, a, r)
    }

    shouldBeGolden(cap.xr, "XR")
    shouldBeGolden(cap.build<PostgresDialect>(), "SQL")
  }
  "sql.expression.use.(@Cap (Row)()->Table)" {
    @SqlFragment
    fun Person.joinAddress() = sql.expression {
      composeFrom.join(Table<Address>()) { a -> this@joinAddress.id == a.ownerId }
    }

    @SqlFragment
    fun Person.joinRobot() = sql.expression {
      composeFrom.join(Table<Robot>()) { r -> this@joinRobot.id == r.ownerId }
    }

    val cap = sql.select {
      val p = from(Table<Person>())
      val a = from(p.joinAddress().use)
      val r = from(p.joinRobot().use)
      Triple(p, a, r)
    }

    shouldBeGolden(cap.xr, "XR")
    shouldBeGolden(cap.build<PostgresDialect>(), "SQL")
  }

  "sql.(@Cap (Row)()->Table)" {
    @SqlFragment
    fun Person.joinAddress() = sql {
      composeFrom.join(Table<Address>()) { a -> this@joinAddress.id == a.ownerId }
    }

    @SqlFragment
    fun Person.joinRobot() = sql {
      composeFrom.join(Table<Robot>()) { r -> this@joinRobot.id == r.ownerId }
    }

    val cap = sql.select {
      val p = from(Table<Person>())
      val a = from(p.joinAddress())
      val r = from(p.joinRobot())
      Triple(p, a, r)
    }

    shouldBeGolden(cap.xr, "XR")
    shouldBeGolden(cap.build<PostgresDialect>(), "SQL")
  }

})
