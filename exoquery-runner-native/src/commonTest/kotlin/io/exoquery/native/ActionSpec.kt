package io.exoquery.native

import io.exoquery.IllegalSqlOperation
import io.exoquery.testdata.Person
import io.exoquery.SqliteDialect
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.sql
import io.exoquery.controller.native.NativeDatabaseController
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.postgres.people
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

infix fun <T> T.shouldBe(expected: T) {
  assertEquals(expected, this)
}

infix fun <T> List<T>.shouldContainExactlyInAnyOrder(expected: List<T>) {
  assertEquals(expected.toSet(), this.toSet())
}

class ActionSpec {
  private val ctx = TestDatabase.ctx

  @OptIn(TerpalSqlUnsafe::class)
  @BeforeTest
  fun setup(): Unit = runBlocking {
    ctx.runActionsUnsafe(
      """
      DELETE FROM Person;
      DELETE FROM Address;
      DELETE FROM Robot;
      """
    )
  }

  @OptIn(TerpalSqlUnsafe::class)
  suspend fun NativeDatabaseController.insertGeorgeAndJim() =
    this.runActionsUnsafe(
      """
        INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'George', 'Googs', 555);
        INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Jim', 'Roogs', 222);
      """.trimIndent()
    )

  val joe = Person(1, "Joe", "Bloggs", 111)
  val george = Person(1, "George", "Googs", 555)
  val jim = Person(2, "Jim", "Roogs", 222)

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
  fun `insert with returning`() = runBlocking {
    val q = sql {
      insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 }
    }
    val build = q.build<SqliteDialect>()
    build.runOn(ctx) shouldBe 101
    ctx.people() shouldBe listOf(joe)
  }

  @Test
  fun `insert with returning using param`() = runBlocking {
    val n = 1000
    val q = sql {
      insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 + param(n) }
    }
    val build = q.build<SqliteDialect>()
    build.runOn(ctx) shouldBe 1101
    ctx.people() shouldBe listOf(joe)
  }

  @Test
  fun `insert with returning - multiple`() = runBlocking {
    val q = sql {
      insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id to p.firstName }
    }
    val build = q.build<SqliteDialect>()
    build.runOn(ctx) shouldBe (1 to "Joe")
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

  @Test
  fun `update simple`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
  }

  @Test
  fun `update no condition`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      update<Person> { set(firstName to param("Joe"), lastName to param("Bloggs"), age to 111) }.all()
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 2
    ctx.people() shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Bloggs", 111)
    )
  }

  @Test
  fun `update with setParams`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val updateCall = Person(1, "Joe", "Bloggs", 111)
    val q = sql {
      // TODO need to make a warning when this situation happens, can't have param instances here
      // update<Person> { setParams(Person(1, param("Joe"), param("Bloggs"), 111)) }.filter { p -> p.id == 1 }
      update<Person> { setParams(updateCall) }.filter { p -> p.id == 1 }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
  }

  @Test
  fun `update with setParams and exclusion`() = runBlocking {
    ctx.insertGeorgeAndJim()
    // Set a large Id that should specifically be excluded from insertion
    val updateCall = Person(1000, "Joe", "Bloggs", 111)
    val q = sql {
      // Set the ID to 0 so we can be sure
      update<Person> { setParams(updateCall).excluding(id) }.filter { p -> p.id == 1 }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
  }

  @Test
  fun `update with returning`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returning { p -> p.id + 100 }
    }
    val build = q.build<SqliteDialect>()
    build.runOn(ctx) shouldBe 101
    ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
  }

  @Test
  fun `update with returning - multiple`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returning { p -> p.id to p.firstName }
    }
    val build = q.build<SqliteDialect>()
    build.runOn(ctx) shouldBe (1 to "Joe")
    ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
  }

  @Test
  fun `update with returningKeys`() = runBlocking {
    val q = sql {
      update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returningKeys { id }
    }
    val build = q.build<SqliteDialect>()
    assertFailsWith<IllegalSqlOperation> {
      build.runOn(ctx)
    }
    Unit
  }

  @Test
  fun `delete simple`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      delete<Person>().filter { p -> p.id == 1 }
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
  }

  @Test
  fun `delete no condition`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      delete<Person>().all()
    }
    q.build<SqliteDialect>().runOn(ctx) shouldBe 2
    ctx.people() shouldBe emptyList()
  }

  @Test
  fun `delete with returning`() = runBlocking {
    ctx.insertGeorgeAndJim()
    val q = sql {
      delete<Person>().filter { p -> p.id == 1 }.returning { p -> p.id + 100 }
    }
    val build = q.build<SqliteDialect>()
    build.runOn(ctx) shouldBe 101
    ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
  }

  @Test
  fun `delete with returningKeys`() = runBlocking {
    val q = sql {
      delete<Person>().filter { p -> p.id == 1 }.returningKeys { id }
    }
    val build = q.build<SqliteDialect>()
    assertFailsWith<IllegalSqlOperation> {
      build.runOn(ctx)
    }
    Unit
  }
}
