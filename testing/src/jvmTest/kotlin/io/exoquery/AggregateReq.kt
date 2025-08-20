package io.exoquery

import io.exoquery.public.Person
import io.exoquery.sql.PostgresDialect

class AggregateReq: GoldenSpecDynamic(AggregateReqGoldenDynamic, Mode.ExoGoldenTest(), {

  "value function aggregate" - {
    "avg" {
      val avgAge = capture.select {
        Table<Person>().map { it.age }.avg()
      }
      shouldBeGolden(avgAge.xr, "XR")
      shouldBeGolden(avgAge.build<PostgresDialect>(), "SQL")
    }

    "avg typed" {
      val avgAge = capture.select {
        Table<Person>().map { it.age }.avg<Int>()
      }
      shouldBeGolden(avgAge.xr, "XR")
      shouldBeGolden(avgAge.build<PostgresDialect>(), "SQL")
    }

    "stdev" {
      val stddevAge = capture.select {
        Table<Person>().map { it.age }.stddev()
      }
      shouldBeGolden(stddevAge.xr, "XR")
      shouldBeGolden(stddevAge.build<PostgresDialect>(), "SQL")
    }

    "stdev typed" {
      val stddevAge = capture.select {
        Table<Person>().map { it.age }.stddev<Int>()
      }
      shouldBeGolden(stddevAge.xr, "XR")
      shouldBeGolden(stddevAge.build<PostgresDialect>(), "SQL")
    }
  }

  "column function aggregate" - {
    "avg" {
      val avgAge = capture.select {
        Table<Person>().map { avg(it.age) }.value()
      }
      shouldBeGolden(avgAge.xr, "XR")
      shouldBeGolden(avgAge.build<PostgresDialect>(), "SQL")
    }

    "avg typed" {
      val avgAge = capture.select {
        Table<Person>().map { avg<Int>(it.age) }.value()
      }
      shouldBeGolden(avgAge.xr, "XR")
      shouldBeGolden(avgAge.build<PostgresDialect>(), "SQL")
    }

    "stdev" {
      val stddevAge = capture.select {
        Table<Person>().map { stddev(it.age) }.value()
      }
      shouldBeGolden(stddevAge.xr, "XR")
      shouldBeGolden(stddevAge.build<PostgresDialect>(), "SQL")
    }

    "stdev typed" {
      val stddevAge = capture.select {
        Table<Person>().map { stddev<Int>(it.age) }.value()
      }
      shouldBeGolden(stddevAge.xr, "XR")
      shouldBeGolden(stddevAge.build<PostgresDialect>(), "SQL")
    }
  }
})
