package io.exoquery

import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person


class SelectClauseQueryReq : GoldenSpecDynamic(SelectClauseQueryReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "table.emb?.field" - {
    data class Name(val first: String, val last: String)
    data class Person(val name: Name?, val age: Int)
    data class Robot(val ownerFirstName: String, val model: String)

    "join(p.name?.first)" {
      val people =
        capture.select {
          val p = from(Table<Person>())
          val r = join(Table<Robot>()) { r -> p.name?.first == r.ownerFirstName }
          p to r
        }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>(), "SQL")
    }
    "join(p.name?.first ?: alternative)" {
      val people =
        capture.select {
          val p = from(Table<Person>())
          val r = join(Table<Robot>()) { r -> p.name?.first ?: "defaultName" == r.ownerFirstName }
          p to r
        }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>(), "SQL")
    }
    "joinLeft(p.name?.first ?: alternative)" {
      val people =
        capture.select {
          val p = from(Table<Person>())
          val r = joinLeft(Table<Robot>()) { r -> p.name?.first ?: "defaultName" == r.ownerFirstName }
          p.name to r?.model
        }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>(), "SQL")
    }

    "joinLeft(p.name?.first ?: alternative) -> r ?: alternative" {
      val people =
        capture.select {
          val p = from(Table<Person>())
          val r = joinLeft(Table<Robot>()) { r -> p.name?.first ?: "defaultName" == r.ownerFirstName }
          // TODO need an unhappy-paths test for this
          //      p.name to (r ?: Robot("defaultName", "defaultModel")).model <- this should be an error because elvis op cannot be called on a product type

          p.name to r?.model
        }.dyanmic()
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>(), "SQL")
    }

    "joinLeft(p.name?.first ?: alternative) -> p ?: alternative" {
      val people =
        capture.select {
          val r = from(Table<Robot>())
          val p = joinLeft(Table<Person>()) { p -> r.ownerFirstName == (p.name?.first ?: "defaultName") }
          // TODO need an unhappy-paths test for this
          //      (p ?: Person(Name("foo", "bar"), 123)).name?.first to r.model
          (p?.name?.first ?: "foo") to r.model
        }.dyanmic()
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>(), "SQL")
    }

    "join + aggregation" {
      val people =
        capture.select {
          val p = from(Table<Person>())
          val r = join(Table<Robot>()) { r -> p.name?.first == r.ownerFirstName }
          groupBy(p.name?.first, r.model)
          p.name?.first to r.model
        }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>(), "SQL")
    }

    "sortBy" {
      val people =
        capture.select {
          val p = from(Table<Person>())
          sortBy(p.age to Ord.Asc, p.name?.first to Ord.Desc)
          // TODO when I have the ability to test compile-errors, should test the error that happnes when I try doing p.name for the order-by expression
          p.name to p.age
        }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>(), "SQL")
    }

  }

  "join + where + groupBy + sortBy" {
    val people =
      capture.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> p.id == a.ownerId }
        where { p.age > 18 }
        groupBy(p.age, a.street)
        sortBy(p.age to Ord.Asc, a.street to Ord.Desc)
        p to a
      }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>(), "SQL")
  }
})
