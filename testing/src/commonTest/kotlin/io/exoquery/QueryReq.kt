package io.exoquery

import io.exoquery.annotation.SqlFragment
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
    }.dynamic()
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
  "select with distinct count" {
    val people = sql.select {
      val p = from(Table<Person>())
      countDistinct(p.name, p.age)
    }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "map with stddev" {
    val people = sql { Table<Person>().map { p -> stddev(p.age) } }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "select with stdev and another column" {
    val people = sql.select {
      val p = from(Table<Person>())
      stddev(p.age) to p.name
    }
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

  "correlated contains" - {
    "correlated contains - simple" {
      val people = sql {
        Table<Person>().filter { p -> p.id in Table<Address>().map { p -> p.ownerId } }
      }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>())
    }
    "correlated contains - different var names 1" {
      val people = sql {
        Table<Person>().filter { p -> p.id in Table<Address>().map { it.ownerId } }
      }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>())
    }
    "correlated contains - different var names 2" {
      val people = sql {
        Table<Person>().filter { p -> p.id in Table<Address>().map { pp -> pp.ownerId } }
      }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>())
    }
    "correlated contains - only it vars" {
      val people = sql {
        Table<Person>().filter { it.id in Table<Address>().map { it.ownerId } }
      }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.build<PostgresDialect>())
    }
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
    @SqlFragment
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
    @SqlFragment
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

  "implicit joins" {
    val q = sql {
      Table<Person>().flatMap { p ->
        Table<Address>().filter { a -> a.ownerId == p.id }.map { a -> p.name to a.city }
      }
    }
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }

  /**
   * BUG REPRODUCTION: Incorrect alias resolution in nested fragment filters
   *
   * When filtering a SqlQuery<CompositeType> through a fragment, field paths like
   * `it.a.fieldName` resolve to the wrong table alias.
   *
   * EXPECTED: `it.a.age` → `person.age` (references Person table with alias 'person')
   * ACTUAL:   `it.a.age` → `address.age` (references Address table with alias 'address')
   *
   * The bug appears to use the last-joined table's alias instead of following
   * the field path through the composite type.
   */
  "nested fragment filter - wrong alias resolution bug" {
    // Composite type holding both Person and Address
    data class Composite(val a: Person, val b: Address)

    // Fragment 1: Join Person and Address into Composite
    @SqlFragment
    fun baseJoin(): SqlQuery<Composite> = sql.select {
      val person = from(Table<Person>())
      val address = join(Table<Address>()) { addr -> addr.ownerId == person.id }
      Composite(person, address)
    }

    // Fragment 2: Filter on field from 'a' component (Person)
    @SqlFragment
    fun filterOnA(base: SqlQuery<Composite>): SqlQuery<Composite> = sql {
      base.filter { it.a.age > 18 }
      //           ^^^^^^^^
      //           Should resolve to: person.age
      //           BUG: Actually resolves to: address.age (wrong table!)
    }

    // Composed query demonstrating the bug
    val query = sql.select {
      val row = from(filterOnA(baseJoin()))
      row
    }

    shouldBeGolden(query.xr, "XR")
    shouldBeGolden(query.build<PostgresDialect>())
  }

  /**
   * BUG REPRODUCTION: Lambda parameter names ignored in composeFrom.join subquery aliases
   *
   * When using composeFrom.join with filtered subqueries, both subqueries get the same
   * alias ("it") despite having explicit lambda parameter names ("b", "c").
   *
   * EXPECTED: Subqueries get distinct aliases matching lambda parameters (AS b, AS c)
   * ACTUAL:   Both subqueries get alias "it" causing SQL naming conflict
   *
   * The lambda parameter name only affects the ON clause reference, not the subquery alias.
   */
  "composeFrom.join - duplicate subquery alias bug" {
    data class A(val id: Long, val bId: Long, val cId: Long)
    data class B(val id: Long, val status: String)
    data class C(val id: Long, val status: String)
    data class Result(val aId: Long, val bId: Long, val cId: Long)

    @SqlFragment
    fun A.activeB() = sql {
      composeFrom.join(Table<B>().filter { it.status == "active" }) { b -> b.id == this@activeB.bId }
    }

    @SqlFragment
    fun A.activeC() = sql {
      composeFrom.join(Table<C>().filter { it.status == "active" }) { c -> c.id == this@activeC.cId }
    }

    val query = sql.select {
      val a = from(Table<A>())
      val b = from(a.activeB())
      val c = from(a.activeC())
      Result(a.id, b.id, c.id)
    }

    shouldBeGolden(query.xr, "XR")
    shouldBeGolden(query.build<PostgresDialect>())
  }

  "PushAlias tests for composeFrom.join" - {
    data class A(val id: Long, val bId: Long, val cId: Long)
    data class B(val id: Long, val status: String, val value: Int)
    data class C(val id: Long, val bId: Long, val name: String)

    "flatJoin with nested select query" {
      @SqlFragment
      fun A.joinWithSelect() = sql {
        composeFrom.join(
          select {
            val bb = from(Table<B>())
            where { bb.status == "active" }
            bb
          }
        ) { selectedB -> selectedB.id == this@joinWithSelect.bId }
      }

      val query = sql.select {
        val a = from(Table<A>())
        val b = from(a.joinWithSelect())
        a.id to b.value
      }

      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.build<PostgresDialect>())
    }

    "flatJoin with map and filter chain" {
      @SqlFragment
      fun A.joinWithMapFilter() = sql {
        composeFrom.join(
          Table<B>()
            .filter { it.status == "active" }
            .map { it }
        ) { filteredB -> filteredB.id == this@joinWithMapFilter.bId }
      }

      val query = sql.select {
        val a = from(Table<A>())
        val b = from(a.joinWithMapFilter())
        a.id to b.value
      }

      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.build<PostgresDialect>())
    }

    "flatJoin with flatMap" {
      @SqlFragment
      fun A.joinWithFlatMap() = sql {
        composeFrom.join(
          Table<B>().flatMap { bb ->
            Table<C>()
              .filter { c -> c.bId == bb.id }
              .map { c -> bb }
          }
        ) { mappedB -> mappedB.id == this@joinWithFlatMap.bId }
      }

      val query = sql.select {
        val a = from(Table<A>())
        val b = from(a.joinWithFlatMap())
        a.id to b.value
      }

      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.build<PostgresDialect>())
    }

    "multiple flatJoins with different query types" {
      @SqlFragment
      fun A.joinSelect() = sql {
        composeFrom.join(
          select {
            val bb = from(Table<B>())
            where { bb.status == "active" }
            bb
          }
        ) { selectedB -> selectedB.id == this@joinSelect.bId }
      }

      @SqlFragment
      fun A.joinFiltered() = sql {
        composeFrom.join(
          Table<C>().filter { it.name == "test" }
        ) { filteredC -> filteredC.bId == this@joinFiltered.cId }
      }

      val query = sql.select {
        val a = from(Table<A>())
        val b = from(a.joinSelect())
        val c = from(a.joinFiltered())
        Triple(a.id, b.value, c.name)
      }

      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.build<PostgresDialect>())
    }

    "multiple flatJoins with different query types - and a duplicate" {
      @SqlFragment
      fun A.joinSelect() = sql {
        composeFrom.join(
          select {
            val bb = from(Table<B>())
            where { bb.status == "active" }
            bb
          }
        ) { selectedB -> selectedB.id == this@joinSelect.bId }
      }

      @SqlFragment
      fun A.joinFiltered() = sql {
        composeFrom.join(
          Table<C>().filter { it.name == "test" }
        ) { filteredC -> filteredC.bId == this@joinFiltered.cId }
      }

      val query = sql.select {
        val a = from(Table<A>())
        val b = from(a.joinSelect())
        val b1 = from(a.joinSelect())
        val c = from(a.joinFiltered())
        Triple(a.id, b.value + b1.value, c.name)
      }

      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.build<PostgresDialect>())
    }

    "flatJoin with complex nested select" {
      @SqlFragment
      fun A.complexJoin() = sql {
        composeFrom.join(
          select {
            val bb = from(Table<B>())
            where { bb.status == "active" }
            groupBy(bb.status)
            having { avg(bb.value) > 10 }
            sortBy(bb.status to Ord.Asc)
            bb
          }
        ) { complexB -> complexB.id == this@complexJoin.bId }
      }

      val query = sql.select {
        val a = from(Table<A>())
        val b = from(a.complexJoin())
        a.id to b.value
      }

      shouldBeGolden(query.xr, "XR")
      shouldBeGolden(query.build<PostgresDialect>())
    }
  }

  /**
   * BUG REPRODUCTION: Incorrect FROM clause generation for filtered joined fragments
   *
   * When using a filtered joined fragment in a select query with a where clause,
   * the FROM clause incorrectly generates both a plain table reference AND a subquery.
   *
   * EXPECTED: FROM (SELECT ... FROM A INNER JOIN B ...)
   * ACTUAL:   FROM A a, (SELECT ... FROM A INNER JOIN B ...)
   *
   * The bug causes the base table to appear twice in the FROM clause when applying
   * a filter to a joined fragment and then using it in a select with additional filters.
   */
  "filtered joined fragment - duplicate table in FROM clause bug" {
    data class A(val id: Int)
    data class B(val id: Int, val aId: Int)
    data class Composite(val a: A, val b: B)

    @SqlFragment fun joined(): SqlQuery<Composite> = sql.select {
      val a = from(Table<A>())
      val b = join(Table<B>()) { b -> b.aId == a.id }
      Composite(a, b)
    }

    @SqlFragment fun SqlQuery<Composite>.filtered(): SqlQuery<Composite> = sql {
      this@filtered.filter { it.a.id > 0 }
    }

    val buggy = sql.select {
      val r = from(joined().filtered())
      where { r.b.id > 0 }  // triggers: FROM A a, (SELECT ... FROM INNER JOIN B ...)
      r.a.id
    }

    shouldBeGolden(buggy.xr, "XR")
    shouldBeGolden(buggy.build<PostgresDialect>())
  }

  "nested select with filter on nested pair - double nested" {
    val nestedSelect2 = sql.select {
      val p = from(Table<PersonCrs>())
      val a = join(Table<AddressCrs>()) { it.ownerId == p.id }
      p to a
    }

    val nestedSelect =
      sql.select {
        val p = from(nestedSelect2.nested())
        val a = join(Table<AddressCrs>()) { it.ownerId == p.first.id }
        p to a
      }

    val q = sql {
      nestedSelect.filter { pair -> pair.first.first.name == "JoeOuter" }
    }.dynamic()

    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }

  "nested select with filter on pair - single nested" {
    val nestedSelect =
      sql.select {
        val p = from(Table<PersonCrs>())
        val a = join(Table<AddressCrs>()) { it.ownerId == p.id }
        p to a
      }

    val q = sql {
      nestedSelect.filter { pair -> pair.first.name == "JoeOuter" }
    }.dynamic()

    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }

  "select with where groupBy and left join - filter on grouped field" {
    val people = sql { Table<Person>() }
    val addresses = sql { Table<Address>() }

    val c = sql {
      select {
        val p = from(people)
        val a = joinLeft(addresses) { it.ownerId == p.id }
        where { p.age > 18 }
        groupBy(p)
        p
      }.filter { ccc -> ccc.name == "Main St" }
    }

    shouldBeGolden(c.xr, "XR")
    shouldBeGolden(c.build<PostgresDialect>())
  }

})
