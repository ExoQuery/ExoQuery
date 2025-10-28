package io.exoquery

import io.exoquery.annotation.CapturedFunction
import io.exoquery.PostgresDialect
import io.exoquery.testdata.Robot

// Note that the 1st time you overwrite the golden file it will still fail because the compiler is using the old version
// Also note that it won't actually override the BasicQuerySanitySpecGolden file unless you change this one

// build the file BasicQuerySanitySpecGolden.kt, is that as the constructor arg
class QueryReq: GoldenSpecDynamic(QueryReqGoldenDynamic, Mode.ExoGoldenTest(), {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val ownerId: Int, val street: String, val city: String)

  "basic query" {
    val people = sql { Table<Person>() }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "basic query - deprecated capture" {
    val people = capture { Table<Person>() }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with map" {
    val people = sql { Table<Person>().map { p -> p.name } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with groupBy" {
    val people = sql.select {
      val p = from(Table<Person>())
      groupBy(p.name)
      p.name to avg(p.age)
    }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with groupBy + having" {
    val people = sql.select {
      val p = from(Table<Person>())
      groupBy(p.name)
      having { avg(p.age) > 18 }
      p.name to avg(p.age)
    }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with groupBy + having + orderBy" {
    val people = sql.select {
      val p = from(Table<Person>())
      groupBy(p.name)
      having { avg(p.age) > 18 }
      orderBy(avg(p.age) to Ord.Desc)
      p.name to avg(p.age)
    }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with with-clause + groupBy + having" {
    val people = sql.select {
      val p = from(Table<Person>())
      where { p.name == "Joe" }
      groupBy(p.name)
      having { avg(p.age) > 18 }
      p.name to avg(p.age)
    }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "union with impure free - should not collapse" {
    val joes = sql { Table<Person>().filter { p -> p.name == "Joe" } }
    val jacks = sql { Table<Person>().filter { p -> p.name == "Jack" } }
    val people = sql.select {
      val u = from(joes union jacks)
      val a = join(Table<Address>()) { a -> a.ownerId == u.id }
      free("stuff(${u.name})")<String>()
    }.dyanmic()
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "union with pure function - should collapse" {
    val joes = sql { Table<Person>().filter { p -> p.name == "Joe" } }
    val jacks = sql { Table<Person>().filter { p -> p.name == "Jack" } }
    val people = sql.select {
      val u = from(joes union jacks)
      val a = join(Table<Address>()) { a -> a.ownerId == u.id }
      u.name.uppercase()
    }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "union with aggregation - shuold not collapse" {
    val joes = sql { Table<Person>().filter { p -> p.name == "Joe" } }
    val jacks = sql { Table<Person>().filter { p -> p.name == "Jack" } }
    val people = sql.select {
      val u = from(joes union jacks)
      val a = join(Table<Address>()) { a -> a.ownerId == u.id }
      avg(u.age)
    }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "map with aggregation" {
    val people = sql { Table<Person>().map { p -> avg(p.age) } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "map with count" {
    val people = sql { Table<Person>().map { p -> count(p.age) } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "map with count star" {
    val people = sql { Table<Person>().map { p -> count() } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "map with count distinct" {
    val people = sql { Table<Person>().map { p -> countDistinct(p.name, p.age) } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "map with stddev" {
    val people = sql { Table<Person>().map { p -> stddev(p.age) } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with filter" {
    val people = sql { Table<Person>().filter { p -> p.age > 18 } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with where" {
    val people = sql { Table<Person>().where { age > 18 } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }

  "filter + correlated isEmpty" {
    val people = sql { Table<Person>().filter { p -> p.age > Table<Person>().map { p -> p.age }.avg() } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }

  "filter + correlated + value" {
    val people =
      sql { Table<Person>().filter { p -> p.age > Table<Person>().map { p -> avg(p.age) - min(p.age) }.value() } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }

  "query with flatMap" {
    val people = sql { Table<Person>().flatMap { p -> Table<Address>().filter { a -> a.ownerId == p.id } } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with union" {
    val people =
      sql { (Table<Person>().filter { p -> p.age > 18 } union Table<Person>().filter { p -> p.age < 18 }) }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with unionAll" {
    val people =
      sql { (Table<Person>().filter { p -> p.age > 18 } unionAll Table<Person>().filter { p -> p.age < 18 }) }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with unionAll - symbolic" {
    val people = sql { (Table<Person>().filter { p -> p.age > 18 } + Table<Person>().filter { p -> p.age < 18 }) }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with surrounding free" {
    val q = sql {
      free("${Table<Person>().filter { p -> p.name == "Joe" }} FOR UPDATE").asPure<SqlQuery<Person>>()
    }
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }
  "query with free in captured function" {
    @CapturedFunction
    fun <T> forUpdate(v: SqlQuery<T>) = sql {
      free("${v} FOR UPDATE").asPure<SqlQuery<T>>()
    }

    val q = sql {
      forUpdate(Table<Person>().filter { p -> p.age > 21 })
    }
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }
  "query with free in captured function - receiver position" {
    @CapturedFunction
    fun <T> SqlQuery<T>.forUpdate() = sql {
      free("${this@forUpdate} FOR UPDATE").asPure<SqlQuery<T>>()
    }
    val q = sql {
      Table<Person>().filter { p -> p.age > 21 }.forUpdate()
    }
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }
  "flat joins inside subquery" - {
    // This tests the `is SX.Where` case in XR.SelectClauseToXR.nest making sure that the prevVar is properly passed to outer subclauses
    "where" {
      val q = sql.select {
        val p = from(Table<io.exoquery.testdata.Person>())
        val innerRobot = join(
          sql.select {
            val r = from(Table<Robot>())
            where { r.ownerId == p.id }
            r.name to r.ownerId
          }
        ) { r -> r.second == p.id }
        p to innerRobot
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>())
    }
    // This tests the `is SX.GroupBy` case in XR.SelectClauseToXR.nest making sure that the prevVar is properly passed to outer subclauses
    "groupBy" {
      val q = sql.select {
        val p = from(Table<io.exoquery.testdata.Person>())
        val innerRobot = join(
          sql.select {
            val r = from(Table<Robot>())
            groupBy(r.ownerId)
            r.name to r.ownerId
          }
        ) { r -> r.second == p.id }
        p to innerRobot
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>())
    }
    // This tests the `is SX.SortBy` case in XR.SelectClauseToXR.nest making sure that the prevVar is properly passed to outer subclauses
    "sortBy" {
      val q = sql.select {
        val p = from(Table<io.exoquery.testdata.Person>())
        val innerRobot = join(
          sql.select {
            val r = from(Table<Robot>())
            sortBy(r.name to Ord.Desc)
            r.name to r.ownerId
          }
        ) { r -> r.second == p.id }
        p to innerRobot
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }

  "transformation of nested select clauses" - {
    "where clauses are combined" {
      val addresses = sql.select {
        val a = from(Table<Address>())
        where { a.city == "Someplace" }
        a
      }
      val q = sql.select {
        val p = from(Table<Person>())
        val a = from(addresses)
        where { p.name == "Joe" }
        p to a
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "groupBy clauses cause nesting" - {
      "variation A" {
        val addresses = sql.select {
          val a = from(Table<Address>())
          groupBy(a.city)
          a
        }
        val q = sql.select {
          val p = from(Table<Person>())
          val a = from(addresses)
          where { p.name == "Joe" }
          p to a
        }
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(q.build<PostgresDialect>())
      }
      "Variation B" {
        val addresses = sql.select {
          val a = from(Table<Address>())
          groupBy(a.city)
          a
        }
        val q = sql.select {
          val p = from(Table<Person>())
          val a = from(addresses)
          groupBy(p.name)
          p to a
        }
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(q.build<PostgresDialect>())
      }
    }

    "sortBy clauses cause nesting" - {
      "variation A" {
        val addresses = sql.select {
          val a = from(Table<Address>())
          sortBy(a.city to Ord.Asc)
          a
        }
        val q = sql.select {
          val p = from(Table<Person>())
          val a = from(addresses)
          where { p.name == "Joe" }
          p to a
        }
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(q.build<PostgresDialect>())
      }
      "Variation B" {
        val addresses = sql.select {
          val a = from(Table<Address>())
          sortBy(a.city to Ord.Asc)
          a
        }
        val q = sql.select {
          val p = from(Table<Person>())
          val a = from(addresses)
          sortBy(p.name to Ord.Desc)
          p to a
        }
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(q.build<PostgresDialect>())
      }
    }

    "combo of all cases will cause nesting" {
      val addresses = sql.select {
        val a = from(Table<Address>())
        where { a.city == "Someplace" }
        groupBy(a.city)
        sortBy(a.street to Ord.Asc)
        a
      }
      val q = sql.select {
        val p = from(Table<Person>())
        val a = from(addresses)
        where { p.name == "Joe" }
        groupBy(p.name)
        sortBy(p.age to Ord.Desc)
        p to a
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }

})
