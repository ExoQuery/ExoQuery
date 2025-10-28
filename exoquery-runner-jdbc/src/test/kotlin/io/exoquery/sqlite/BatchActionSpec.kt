package io.exoquery.sqlite

import io.exoquery.testdata.Person
import io.exoquery.SqliteDialect
import io.exoquery.TestDatabases
import io.exoquery.allPeople
import io.exoquery.batchDeletePeople
import io.exoquery.batchInsertPeople
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.george
import io.exoquery.insertAllPeople
import io.exoquery.insertPerson
import io.exoquery.joe
import io.exoquery.people
import io.exoquery.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class BatchActionSpec : FreeSpec({
  val ctx = TestDatabases.sqlite

  beforeEach {
    ctx.runActions(
      // The `test` table is used to determine column increments, drop it to reset the IDs. However, in case it doesn't exist yet we need to create it.
      """
      CREATE TABLE test(id INTEGER PRIMARY KEY AUTOINCREMENT); DROP TABLE test;
      DELETE FROM Person;
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
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "simple with setParams" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { setParams(p) }
      }
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "simple with setParams and exclusion" {
      ctx.insertPerson(joe)
      // Modify the ids to make sure it is inserting records with a new Id, not the ones used here
      val insertPeople = batchInsertPeople.map { it.copy(id = it.id + 100) }.asSequence()
      val q = sql.batch(insertPeople) { p ->
        insert<Person> { setParams(p).excluding(id) }
      }
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "with returning" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returning { p -> p.id + 100 }
      }
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(102, 103, 104)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "with returning and params" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returning { pp -> pp.id + 100 to param(p.firstName) }
      }
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf((102 to "Joe"), (103 to "Jim"), (104 to "George"))
      ctx.people() shouldContainExactlyInAnyOrder people + george
    }
    // Not supported for Sqlite
    //"with returning keys" {
    //  ctx.insertPerson(joe)
    //  val q = sql.batch(batchInsertPeople.asSequence()) { p ->
    //    insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returningKeys { id }
    //  }
    //  q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
    //  ctx.people() shouldContainExactlyInAnyOrder allPeople
    //}
  }

  "update" - {
    val updatedPeople = listOf(Person(1, "Joe-A", "Bloggs", 112), Person(2, "Joe-A", "Doggs", 222), Person(3, "Jim-A", "Roogs", 333))
    val allNewPeople = updatedPeople + george

    "simple" {
      ctx.insertAllPeople()
      val q = sql.batch(updatedPeople.asSequence()) { p ->
        update<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.filter { pp -> pp.id == param(p.id) }
      }
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    "simple with setParams and exclusion" {
      ctx.insertAllPeople()
      val peopleWithOddIds = updatedPeople.asSequence().map { it.copy(id = it.id + 100) }
      val q = sql.batch(peopleWithOddIds) { p ->
        update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.lastName == param(p.lastName) }
      }
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    "simple with setParams and exclusion and returning param" {
      ctx.insertAllPeople()
      val peopleWithOddIds = updatedPeople.asSequence().map { it.copy(id = it.id + 100) }
      val q = sql.batch(peopleWithOddIds) { p ->
        update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.lastName == param(p.lastName) }.returning { pp -> pp.id to param(p.firstName) }
      }
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1 to "Joe-A", 2 to "Joe-A", 3 to "Jim-A")
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    // Not supported in Sqlite
    //"returningKeys" {
    //  ctx.insertAllPeople()
    //  val q = sql.batch(updatedPeople.asSequence()) { p ->
    //    update<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.filter { pp -> pp.id == param(p.id) }.returningKeys { id }
    //  }
    //  q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
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
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    "using whole object" {
      ctx.insertAllPeople()
      val q = sql.batch(batchDeletePeople.asSequence()) { p ->
        delete<Person>().filter { pp -> pp.id == param(p.id) }
      }
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    "with returning" {
      ctx.insertAllPeople()
      val q = sql.batch(ids) { id ->
        delete<Person>().filter { pp -> pp.id == param(id) }.returning { pp -> pp.id }
      }
      q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    // Not supported in Sqlite
    //"with returning keys" {
    //  ctx.insertAllPeople()
    //  val q = sql.batch(ids) { pid ->
    //    delete<Person>().filter { pp -> pp.id == param(pid) }.returningKeys { id }
    //  }
    //  q.build<SqliteDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    //}
  }
})
