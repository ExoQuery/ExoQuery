package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person

class QueryDistinctReq: GoldenSpecDynamic(QueryDistinctReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "distinct simple" - {
    "table.map.distinct" {
      val q = sql {
        Table<Person>().map { it.name to it.age }.distinct()
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "table.map.distinct.sortBy" {
      val q = sql {
        Table<Person>().map { it.name to it.age }.distinct().sortedBy { it.first }
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "(selectClause + aggregation).distinct" {
      val q = sql {
        select {
          val p = from(Table<Person>())
          val a = join(Table<Address>()) { p.id == it.ownerId }
          groupBy(p.name)
          p.name to max(p.age)
        }.distinct()
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "table.join(table.distinct)" {
      val q = sql.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>().distinct()) { p.id == it.ownerId }
        p to a
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "from.distinct, from" {
      val q = sql.select {
        val p = from(Table<Person>().distinct())
        val a = from(Table<Address>().filter { it.ownerId == p.id })
        p to a
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "from.distinct, sort.join" {
      val q = sql.select {
        val p = from(Table<Person>().distinct())
        val a = join(Table<Address>().sortedBy { it.city }) { p.id == it.ownerId }
        p to a
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
  }
})
