package io.exoquery.android

import io.exoquery.testdata.Person
import io.exoquery.SqliteDialect
import io.exoquery.sql
import io.exoquery.controller.android.AndroidDatabaseController
import io.exoquery.controller.runActions
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActionSpec {
  private val ctx = TestDatabase.ctx

  @Before
  fun setup(): Unit = runBlocking {
    ctx.runActions(
      """
            DELETE FROM Person;
            DELETE FROM Address;
            DELETE FROM Robot;
            """
    )
  }

  @Test
  fun `insert simple`() = runBlocking {
    val q = sql {
      insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldBe listOf(joe)
  }

  @Test
  fun `insert simple with params`() = runBlocking {
    val q = sql {
      insert<Person> { set(firstName to param("Joe"), lastName to param("Bloggs"), age to param(111)) }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldBe listOf(joe)
  }

  @Test
  fun `insert simple with setParams`() = runBlocking {
    val q = sql {
      insert<Person> { setParams(Person(1, "Joe", "Bloggs", 111)) }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldBe listOf(joe)
  }

  @Test
  fun `insert simple with setParams and exclusion`() = runBlocking {
    val q = sql {
      insert<Person> { setParams(Person(1, "Joe", "Bloggs", 111)).excluding(id) }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldBe listOf(joe)
  }

  @Test
  fun `insert with returning keys`() = runBlocking {
    val q = sql {
      insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returningKeys { id }
    }
    val build = q.build<SqliteDialect>()
    build.runOn(ctx) shouldBe 1
    ctx.people() shouldBe listOf(joe)
  }

  val joe = Person(1, "Joe", "Bloggs", 111)
  val george = Person(1, "George", "Googs", 555)
  val jim = Person(2, "Jim", "Roogs", 222)

  @Test
  fun `update simple`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people().toSet() shouldBe setOf(joe, jim)
  }

  @Test
  fun `update no condition`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      update<Person> { set(firstName to param("Joe"), lastName to param("Bloggs"), age to 111) }.all()
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 2
    ctx.people().toSet() shouldBe setOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Bloggs", 111)
    )
  }

  @Test
  fun `delete simple`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      delete<Person>().filter { p -> p.id == 1 }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldBe listOf(jim)
  }

  @Test
  fun `delete no condition`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      delete<Person>().all()
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 2
    ctx.people().isEmpty() shouldBe true
  }


  private suspend fun AndroidDatabaseController.insertGeorgeAndJim() =
    this.runActions(
      """
            INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'George', 'Googs', 555);
            INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Jim', 'Roogs', 222);
            """.trimIndent()
    )

  // Robolectric uses sqlite4java which is stuck an the ancient sqlite 3.8.7 version which doesn't support RETURNING.
  //@Test
  //fun `insert with returning`() = runBlocking {
  //  val q = sql {
  //    insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 }
  //  }
  //  val build = q.build<SqliteDialect>()
  //  build.runOn(ctx) shouldBe 101
  //  ctx.people() shouldBe listOf(joe)
  //}
  //@Test
  //fun `insert with returning using param`() = runBlocking {
  //  val n = 1000
  //  val q = sql {
  //    insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 + param(n) }
  //  }
  //  val build = q.build<SqliteDialect>()
  //  build.runOn(ctx) shouldBe 1101
  //  ctx.people() shouldBe listOf(joe)
  //}
  //@Test
  //fun `insert with returning - multiple`() = runBlocking {
  //  val q = sql {
  //    insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id to p.firstName }
  //  }
  //  val build = q.build<SqliteDialect>()
  //  build.runOn(ctx) shouldBe (1 to "Joe")
  //  ctx.people() shouldBe listOf(joe)
  //}
  //@Test
  //fun `delete returning`() = runBlocking {
  //  ctx.insertGeorgeAndJim()
  //  val q = sql {
  //    delete<Person>().filter { p -> p.id == 1 }.returning { p -> p.firstName }
  //  }
  //  q.build<SqliteDialect>().runOn(ctx) shouldBe "George"
  //  ctx.people() shouldBe listOf(jim)
  //}
}
