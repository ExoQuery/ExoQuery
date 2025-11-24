package io.exoquery.android

import io.exoquery.testdata.Person
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.controller.transaction
import io.exoquery.PostgresDialect
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class TransactionSpec {

  private val ctx = TestDatabase.ctx

  @Before
  fun setup(): Unit = runBlocking {
    ctx.runActions(
      """
            DELETE FROM Person;
            DELETE FROM Address;
            """
    )
  }

  private val joe = Person(1, "Joe", "Bloggs", 111)
  private val jack = Person(2, "Jack", "Roogs", 222)

  private suspend fun select() =
    sql { Table<Person>() }.build<PostgresDialect>().runOn(ctx)

  @Test
  fun `transaction success`() = runBlocking {
    ctx.transaction {
      sql {
        insert<Person> { setParams(joe) }
      }.build<PostgresDialect>().runOnTransaction()
    }
    select() shouldBe listOf(joe)
  }

  @Test
  fun `transaction failure`() = runBlocking {
    // Insert joe record
    sql { insert<Person> { setParams(joe) } }.build<PostgresDialect>().runOn(ctx)

    kotlin.test.assertFailsWith<IllegalStateException> {
      ctx.transaction {
        sql {
          insert<Person> { setParams(jack) }
        }.build<PostgresDialect>().runOnTransaction()
        throw IllegalStateException()
      }
    }
    select() shouldBe listOf(joe)
  }

  @Test
  fun `nested transaction`() = runBlocking {
    val cap = sql {
      insert<Person> { setParams(joe) }
    }.build<PostgresDialect>()

    ctx.transaction {
      ctx.transaction {
        cap.runOnTransaction()
      }
    }
    select() shouldBe listOf(joe)
  }
}
