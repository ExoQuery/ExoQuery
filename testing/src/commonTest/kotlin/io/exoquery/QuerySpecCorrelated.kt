package io.exoquery

import io.exoquery.sql.PostgresDialect

// Note that the 1st time you overwrite the golden file it will still fail because the compile is using teh old version
// Also note that it won't actually override the BasicQuerySanitySpecGolden file unless you change this one

// build the file BasicQuerySanitySpecGolden.kt, is that as the constructor arg
class QuerySpecCorrelated : GoldenSpecDynamic(QuerySpecCorrelatedGoldenDynamic, Mode.ExoGoldenTest(), {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val ownerId: Int, val street: String, val city: String)


  "query with co-releated in filter - isNotEmpty" {
    val people =
      capture { Table<Person>().filter { p -> Table<Address>().filter { a -> a.ownerId == p.id }.isNotEmpty() } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }

  "query with co-releated in filter - isEmpty" {
    val people =
      capture { Table<Person>().filter { p -> Table<Address>().filter { a -> a.ownerId == p.id }.isEmpty() } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }

  // Single-value expressions are not supported yet (i.e. this is the equlivalent of run-query-single)
  //"query aggregation - top-level" - {
  //  "min" {
  //    val people = capture { Table<Address>().map { a -> a.ownerId }.min() }
  //    shouldBeGolden(people.xr, "XR")
  //    shouldBeGolden(people.buildRuntime(PostgresDialect(), "test"))
  //  }
  //}

  "query aggregation" - {
    "min" {
      val people = capture { Table<Person>().filter { p -> Table<Address>().map { a -> a.ownerId }.min() == p.id } }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.buildRuntime(PostgresDialect(), "test"))
    }
  }

  "select aggregation" - {
    "min" {
      val people = capture { Table<Address>().map { a -> min(a.ownerId) } }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>())
    }
  }
})
