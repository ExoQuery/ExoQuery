package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person

class BlockStatementReq: GoldenSpecDynamic(BlockStatementReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "blocks with map" {
    val p = sql {
      Table<Person>().map {
        val name = it.name
        val age = it.age
        name to age
      }
    }
    shouldBeGolden(p.xr, "XR")
    shouldBeGolden(p.build<PostgresDialect>(), "SQL")
  }
  "block with filter condition" {
    val p = sql {
      Table<Person>().filter {
        val age = it.age
        val thirty = 30
        age > thirty
      }
    }
    shouldBeGolden(p.xr, "XR")
    shouldBeGolden(p.build<PostgresDialect>(), "SQL")
  }
  "block in query" {
    val p = sql {
      val p = Table<Person>()
      p.filter { it.age > 30 }
    }
    shouldBeGolden(p.xr, "XR")
    shouldBeGolden(p.build<PostgresDialect>(), "SQL")
  }
  "block with query clause" {
    val p = sql {
      val p = Table<Person>()
      select {
        val p = from(p)
        val a = join(Table<Address>()) { p.id == it.ownerId }
        p to a
      }
    }
    shouldBeGolden(p.xr, "XR")
    shouldBeGolden(p.build<PostgresDialect>(), "SQL")
  }

})
