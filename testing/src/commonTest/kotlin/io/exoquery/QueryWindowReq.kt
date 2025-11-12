package io.exoquery

import io.exoquery.PostgresDialect

class QueryWindowReq: GoldenSpecDynamic(QueryWindowReqGoldenDynamic, Mode.ExoGoldenTest(), {
  data class Person(val id: Int, val name: String, val age: Int)
  "paritionBy, orderBy" - {
    "rank" {
      val q = sql.select {
        val p = from(Table<Person>())
        Pair(
          p.name,
          over().partitionBy(p.name).sortBy(p.age).rank()
        )
      }.dynamic()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "avg" {
      val q = sql.select {
        val p = from(Table<Person>())
        Pair(
          p.name,
          over().partitionBy(p.name).sortBy(p.age).avg(p.id)
        )
      }.dynamic()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "rowNumber" {
      val q = sql.select {
        val p = from(Table<Person>())
        Pair(
          p.name,
          over().partitionBy(p.name).sortBy(p.age).rowNumber()
        )
      }.dynamic()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "count" {
      val q = sql.select {
        val p = from(Table<Person>())
        Pair(
          p.name,
          over().partitionBy(p.name).sortBy(p.age).count(p.age)
        )
      }.dynamic()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "count star" {
      val q = sql.select {
        val p = from(Table<Person>())
        Pair(
          p.name,
          over().partitionBy(p.name).sortBy(p.age).count()
        )
      }.dynamic()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
  }
  "just partitionBy" - {
    "rank" {
      val q = sql.select {
        val p = from(Table<Person>())
        Pair(
          p.name,
          over().partitionBy(p.name).rank()
        )
      }.dynamic()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
  }
  "just orderBy" - {
    "rank" {
      val q = sql.select {
        val p = from(Table<Person>())
        Pair(
          p.name,
          over().sortBy(p.age).rank()
        )
      }.dynamic()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
  }
  "empty over" {
    val q = sql.select {
      val p = from(Table<Person>())
      Pair(
        p.name,
        over().rank()
      )
    }.dynamic()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }
})
