package io.exoquery

import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person

class SqlUdfReq: GoldenSpecDynamic(SqlUdfReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "does necessary casts" {
    val q = capture {
      Table<Person>().map { p -> p.age.toString() to p.name.toInt() }
    }.dyanmic()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }
  "can handle de-nulling - element" {
    data class Test(val id: Int, val name: String?)

    val q = capture {
      Table<Test>().map { p -> p.name!! }
    }.dyanmic()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }
  "can handle de-nulling - row" {
    data class Test(val id: Int, val name: String?)

    val q =
      capture.select {
        val p = from(Table<Person>())
        val a = joinLeft(Table<Address>()) { a -> a.ownerId == p.id }
        p.name to a!!.city
      }
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

})
