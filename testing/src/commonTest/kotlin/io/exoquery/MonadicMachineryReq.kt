package io.exoquery

import io.exoquery.annotation.CapturedFunction
import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person
import io.exoquery.testdata.Robot

class MonadicMachineryReq: GoldenSpecDynamic(MonadicMachineryReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "capture.expression.(Row)->Table" {
    val joinAddress = capture.expression {
      { p: Person -> internal.flatJoin(Table<Address>()) { a -> p.id == a.ownerId } }
    }

    val joinRobot = capture.expression {
      { p: Person -> internal.flatJoin(Table<Robot>()) { r -> p.id == r.ownerId } }
    }

    val cap = capture.select {
      val p = from(Table<Person>())
      val a = from(joinAddress.use(p))
      val r = from(joinRobot.use(p))
      Triple(p, a, r)
    }

    shouldBeGolden(cap.xr, "XR")
    shouldBeGolden(cap.build<PostgresDialect>(), "SQL")
  }
  "capture.expression.(@Cap (Row)->Table).use" {
    @CapturedFunction
    fun joinAddress(p: Person) = capture.expression {
      internal.flatJoin(Table<Address>()) { a -> p.id == a.ownerId }
    }

    @CapturedFunction
    fun joinRobot(p: Person) = capture.expression {
      internal.flatJoin(Table<Robot>()) { r -> p.id == r.ownerId }
    }

    val cap = capture.select {
      val p = from(Table<Person>())
      val a = from(joinAddress(p).use)
      val r = from(joinRobot(p).use)
      Triple(p, a, r)
    }

    shouldBeGolden(cap.xr, "XR")
    shouldBeGolden(cap.build<PostgresDialect>(), "SQL")
  }
  "capture.expression.use.(@Cap (Row)()->Table)" {
    @CapturedFunction
    fun Person.joinAddress() = capture.expression {
      internal.flatJoin(Table<Address>()) { a -> this@joinAddress.id == a.ownerId }
    }

    @CapturedFunction
    fun Person.joinRobot() = capture.expression {
      internal.flatJoin(Table<Robot>()) { r -> this@joinRobot.id == r.ownerId }
    }

    val cap = capture.select {
      val p = from(Table<Person>())
      val a = from(p.joinAddress().use)
      val r = from(p.joinRobot().use)
      Triple(p, a, r)
    }

    shouldBeGolden(cap.xr, "XR")
    shouldBeGolden(cap.build<PostgresDialect>(), "SQL")
  }

  "capture.(@Cap (Row)()->Table)" {
    @CapturedFunction
    fun Person.joinAddress() = capture {
      internal.flatJoin(Table<Address>()) { a -> this@joinAddress.id == a.ownerId }
    }

    @CapturedFunction
    fun Person.joinRobot() = capture {
      internal.flatJoin(Table<Robot>()) { r -> this@joinRobot.id == r.ownerId }
    }

    val cap = capture.select {
      val p = from(Table<Person>())
      val a = from(p.joinAddress())
      val r = from(p.joinRobot())
      Triple(p, a, r)
    }

    shouldBeGolden(cap.xr, "XR")
    shouldBeGolden(cap.build<PostgresDialect>(), "SQL")
  }

})
