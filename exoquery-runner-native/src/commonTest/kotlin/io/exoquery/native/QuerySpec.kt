package io.exoquery.native

import io.exoquery.testdata.Address
import io.exoquery.Ord
import io.exoquery.testdata.Person
import io.exoquery.SqliteDialect
import io.exoquery.testdata.Robot
import io.exoquery.capture
import io.exoquery.controller.runActions
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.to

class QuerySpec {
  private val ctx = TestDatabase.ctx

  @BeforeTest
  fun setup() = runBlocking {
    ctx.runActions(
      """
      DELETE FROM Person;
      DELETE FROM Address;
      DELETE FROM Robot;
      INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'Joe', 'Bloggs', 111);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Joe', 'Doggs', 222);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (3, 'Jim', 'Roogs', 333);
      INSERT INTO Address (ownerId, street, zip) VALUES (1, '123 Main St', '12345');
      INSERT INTO Address (ownerId, street, zip) VALUES (1, '456 Elm St', '67890');
      INSERT INTO Address (ownerId, street, zip) VALUES (2, '789 Oak St', '54321');
      INSERT INTO Robot (ownerId, model, age) VALUES (2, 'R2D2', 22);
      INSERT INTO Robot (ownerId, model, age) VALUES (3, 'C3PO', 33);
      INSERT INTO Robot (ownerId, model, age) VALUES (3, 'T100', 44);
      """
    )
    Unit
  }

  @Test
  fun `simple`() = runBlocking {
    val q = capture { Table<Person>() }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222),
      Person(3, "Jim", "Roogs", 333)
    )
  }

  @Test
  fun `filter`() = runBlocking {
    val q = capture { Table<Person>().filter { it.firstName == "Joe" } }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222)
    )
  }

  @Test
  fun `filter with param`() = runBlocking {
    val joe = "Joe"
    val q = capture { Table<Person>().filter { it.firstName == param(joe) } }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222)
    )
  }

  @Test
  fun `where`() = runBlocking {
    val q = capture { Table<Person>().where { firstName == "Joe" } }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222)
    )
  }

  @Test
  fun `filter + sortedBy`() = runBlocking {
    val q = capture { Table<Person>().filter { it.firstName == "Joe" }.sortedBy { it.age } }
    q.build<SqliteDialect>().runOn(ctx) shouldBe listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222)
    )
  }

  @Test
  fun `filter + correlated isNotEmpty`() = runBlocking {
    val q = capture { Table<Person>().filter { p -> Table<Address>().filter { it.ownerId == p.id }.isNotEmpty() } }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222)
    )
  }

  @Test
  fun `filter + correlated isEmpty`() = runBlocking {
    val q = capture { Table<Person>().filter { p -> Table<Address>().filter { it.ownerId == p.id }.isEmpty() } }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(3, "Jim", "Roogs", 333)
    )
  }

  @Test
  fun `sort + take`() = runBlocking {
    val q = capture { Table<Person>().sortedBy { it.age }.take(1) }
    q.build<SqliteDialect>().runOn(ctx) shouldBe listOf(
      Person(1, "Joe", "Bloggs", 111)
    )
  }

  @Test
  fun `sort + drop`() = runBlocking {
    val q = capture { Table<Person>().sortedBy { it.age }.drop(1) }
    q.build<SqliteDialect>().runOn(ctx) shouldBe listOf(
      Person(2, "Joe", "Doggs", 222),
      Person(3, "Jim", "Roogs", 333)
    )
  }

  @Test
  fun `distinct`() = runBlocking {
    val q = capture { Table<Person>().map { it.firstName }.distinct() }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      "Joe",
      "Jim"
    )
  }

  // Not supported in Sqlite
  //@Test
  //fun `distinctOn`() = runBlocking {
  //  val q = capture { Table<Person>().distinctOn { it.firstName } }
  //  q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
  //    Person(1, "Joe", "Bloggs", 111),
  //    Person(3, "Jim", "Roogs", 333)
  //  )
  //}

  @Test
  fun `sort + drop + take`() = runBlocking {
    val q = capture { Table<Person>().sortedBy { it.age }.drop(1).take(1) }
    q.build<SqliteDialect>().runOn(ctx) shouldBe listOf(
      Person(2, "Joe", "Doggs", 222)
    )
  }

  @Test
  fun `map`() = runBlocking {
    val q = capture { Table<Person>().map { it.firstName to it.lastName } }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      "Joe" to "Bloggs",
      "Joe" to "Doggs",
      "Jim" to "Roogs"
    )
  }

  @Serializable
  data class Name(val first: String, val last: String)

  @Serializable
  data class CustomPerson(val name: Name, val age: Int)

  @Test
  fun `map to custom`() = runBlocking {
    val q = capture { Table<Person>().map { CustomPerson(Name(it.firstName, it.lastName), it.age) } }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      CustomPerson(Name("Joe", "Bloggs"), 111),
      CustomPerson(Name("Joe", "Doggs"), 222),
      CustomPerson(Name("Jim", "Roogs"), 333)
    )
  }

  @Test
  fun `nested`() = runBlocking {
    val q = capture { Table<Person>().nested() }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222),
      Person(3, "Jim", "Roogs", 333)
    )
  }

  @Test
  fun `union`() = runBlocking {
    val bloggs = capture { Table<Person>().filter { it.lastName == "Bloggs" } }
    val doggs = capture { Table<Person>().filter { it.lastName == "Doggs" } }
    val q = capture { bloggs union doggs }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222)
    )
  }

  // TODO probably only need to test for one DB e.g. postgres, move it out
  @Test
  fun `deconstruct columns - from a table`() = runBlocking {
    val names = capture.select {
      val p = from(Table<Person>())
      p.firstName to p.lastName
    }
    val q = capture.select {
      val (first, last) = from(names)
      first + " - " + last
    }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      "Joe - Bloggs",
      "Joe - Doggs",
      "Jim - Roogs"
    )
  }

  @Test
  fun `deconstruct columns - from a table - mapped`() = runBlocking {
    val names = capture.select {
      val p = from(Table<Person>())
      p.firstName to p.lastName
    }
    val q = capture {
      names.map { (first, last) -> first + " - " + last }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      "Joe - Bloggs",
      "Joe - Doggs",
      "Jim - Roogs"
    )
  }

  @Test
  fun `deconstruct join - from a join`() = runBlocking {
    val join =
      capture.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.ownerId == p.id }
        p to a
      }
    val deconstuct =
      capture.select {
        val (p, a) = from(join)
        val r = join(Table<Robot>()) { r -> r.ownerId == p.id }
        Triple(p, a, r)
      }

    deconstuct.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Triple(
        Person(2, "Joe", "Doggs", 222),
        Address(2, "789 Oak St", "54321"),
        Robot(2, "R2D2", 22)
      )
    )
  }

  @Test
  fun `joins_Person_Address_join`() = runBlocking {
    val q = capture.select {
      val p = from(Table<Person>())
      val a = join(Table<Address>()) { a -> a.ownerId == p.id }
      p to a
    }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111) to Address(1, "123 Main St", "12345"),
      Person(1, "Joe", "Bloggs", 111) to Address(1, "456 Elm St", "67890"),
      Person(2, "Joe", "Doggs", 222) to Address(2, "789 Oak St", "54321")
    )
  }

  @Test
  fun `joins_Person_Address_left_join`() = runBlocking {
    val q = capture.select {
      val p = from(Table<Person>())
      val a = joinLeft(Table<Address>()) { a -> a.ownerId == p.id }
      p to a
    }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111) to Address(1, "123 Main St", "12345"),
      Person(1, "Joe", "Bloggs", 111) to Address(1, "456 Elm St", "67890"),
      Person(2, "Joe", "Doggs", 222) to Address(2, "789 Oak St", "54321"),
      Person(3, "Jim", "Roogs", 333) to null
    )
  }

  @Test
  fun `joins_Person_Address_left_join_plus_groupBy_name`() = runBlocking {
    val q = capture.select {
      val p = from(Table<Person>())
      val a = joinLeft(Table<Address>()) { a -> a.ownerId == p.id }
      groupBy(p.firstName)
      Triple(p.firstName, sum(p.age), count(a?.street))
    }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Triple("Joe", 444, 3),
      Triple("Jim", 333, 0)
    )
  }

  @Test
  fun `joins_Person_Address_left_join_plus_groupBy_name_plus_filter`() = runBlocking {
    val q = capture.select {
      val p = from(Table<Person>())
      val a = joinLeft(Table<Address>()) { a -> a.ownerId == p.id }
      where { p.lastName == "Doggs" || p.lastName == "Roogs" }
      groupBy(p.firstName)
      Triple(p.firstName, sum(p.age), count(a?.street))
    }
    q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Triple("Joe", 222, 1),
      Triple("Jim", 333, 0)
    )
  }

  @Test
  fun `joins_Person_address_left_join_plus_groupBy_name_plus_filter_plus_orderBy`() = runBlocking {
    val q = capture.select {
      val p = from(Table<Person>())
      val a = joinLeft(Table<Address>()) { a -> a.ownerId == p.id }
      where { p.lastName == "Doggs" || p.lastName == "Roogs" }
      groupBy(p.firstName, p.age)
      sortBy(p.age to Ord.Desc)
      Triple(p.firstName, p.age, count(a?.street))
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe listOf(
      Triple("Jim", 333, 0),
      Triple("Joe", 222, 1)
    )
  }
}
