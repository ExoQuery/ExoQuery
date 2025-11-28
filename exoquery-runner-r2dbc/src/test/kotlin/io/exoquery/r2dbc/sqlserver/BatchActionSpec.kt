package io.exoquery.r2dbc.sqlserver

import io.exoquery.*
import io.exoquery.controller.r2dbc.R2dbcController
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.controller.runActions
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.jdbc.runOn
import io.exoquery.r2dbc.allPeople
import io.exoquery.r2dbc.batchDeletePeople
import io.exoquery.r2dbc.batchInsertPeople
import io.exoquery.r2dbc.george
import io.exoquery.r2dbc.insertPerson
import io.exoquery.r2dbc.joe
import io.exoquery.r2dbc.people
import io.exoquery.testdata.Person
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.collections.plus

class BatchActionSpec : FreeSpec({
  val ctx = R2dbcControllers.SqlServer(connectionFactory = TestDatabasesR2dbc.sqlServer)

  beforeEach {
    ctx.runActions(
      """
      TRUNCATE TABLE Person; DBCC CHECKIDENT ('Person', RESEED, 1);
      DELETE FROM Address;
      DELETE FROM Robot;
      """
    )
  }

  "insert" - {

    "simple" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    // TODO Catpured functions are not allowed to return SqlActions instances yet. Need to do some refactoring to allow this.
    //@SqlFunction
    //fun <T> SqlAction<Person, T>.allowIdentityInsert() =
    //  sql {
    //    free("SET IDENTITY_INSERT Person ON\n${this}\nSET IDENTITY_INSERT Person OFF").asPure<SqlAction<Person, T>>()
    //  }

    "simple with setParams" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        free("SET IDENTITY_INSERT Person ON\n${insert<Person> { setParams(p) }}\nSET IDENTITY_INSERT Person OFF").asPure<SqlAction<Person, Long>>()
      }
      q.build<SqlServerDialect>().runOn(ctx) // shouldContainExactlyInAnyOrder listOf(1, 1, 1) // R2dbc driver won't return the generated ID when it is being manually inserted
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "simple with setParams and exclusion" {
      ctx.insertPerson(joe)
      // Modify the ids to make sure it is inserting records with a new Id, not the ones used here
      val insertPeople = batchInsertPeople.map { it.copy(id = it.id + 100) }.asSequence()
      val q = sql.batch(insertPeople) { p ->
        insert<Person> { setParams(p).excluding(id) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "with returning" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returning { p -> p.id + 100 }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(102, 103, 104)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "with returning and params" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returning { pp -> pp.id + 100 to param(p.firstName) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf((102 to "Joe"), (103 to "Jim"), (104 to "George"))
      ctx.people() shouldContainExactlyInAnyOrder people + george
    }
    "with returning keys" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returningKeys { id }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(2, 3, 4)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }
  }

  // Override this for SqlServer since we need to set IDENTITY_INSERT ON
  suspend fun R2dbcController.insertAllPeople() =
    allPeople.forEach {
      sql {
        free("SET IDENTITY_INSERT Person ON\n${insert<Person> { setParams(it) }}\nSET IDENTITY_INSERT Person OFF").asPure<SqlAction<Person, Long>>()
      }.build<PostgresDialect>().runOn(this)
    }

  "update" - {
    val updatedPeople = listOf(Person(1, "Joe-A", "Bloggs", 112), Person(2, "Joe-A", "Doggs", 222), Person(3, "Jim-A", "Roogs", 333))
    val allNewPeople = updatedPeople + george

    "simple" {
      ctx.insertAllPeople()
      val q = sql.batch(updatedPeople.asSequence()) { p ->
        update<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.filter { pp -> pp.id == param(p.id) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    "simple with setParams and exclusion" {
      ctx.insertAllPeople()
      val peopleWithOddIds = updatedPeople.asSequence().map { it.copy(id = it.id + 100) }
      val q = sql.batch(peopleWithOddIds) { p ->
        update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.lastName == param(p.lastName) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    "simple with setParams and exclusion and returning param" {
      ctx.insertAllPeople()
      val peopleWithOddIds = updatedPeople.asSequence().map { it.copy(id = it.id + 100) }
      val q = sql.batch(peopleWithOddIds) { p ->
        update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.lastName == param(p.lastName) }.returning { pp -> pp.id to param(p.firstName) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1 to "Joe-A", 2 to "Joe-A", 3 to "Jim-A")
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    // Not supported in SqlServer
    //"returningKeys" {
    //  ctx.insertAllPeople()
    //  val q = sql.batch(updatedPeople.asSequence()) { p ->
    //    update<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.filter { pp -> pp.id == param(p.id) }.returningKeys { id }
    //  }
    //  q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
    //  ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    //}
  }


  "delete" - {
    val ids = listOf(1, 2, 3).asSequence()

    "simple" {
      ctx.insertAllPeople()
      val q = sql.batch(ids) { id ->
        delete<Person>().filter { pp -> pp.id == param(id) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    "using whole object" {
      ctx.insertAllPeople()
      val q = sql.batch(batchDeletePeople.asSequence()) { p ->
        delete<Person>().filter { pp -> pp.id == param(p.id) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    "with returning" {
      ctx.insertAllPeople()
      val q = sql.batch(ids) { id ->
        delete<Person>().filter { pp -> pp.id == param(id) }.returning { pp -> pp.id }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    // Not supported in SqlServer
    //"with returning keys" {
    //  ctx.insertAllPeople()
    //  val q = sql.batch(ids) { pid ->
    //    delete<Person>().filter { pp -> pp.id == param(pid) }.returningKeys { id }
    //  }
    //  q.build<SqlServerDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    //}
  }
})
