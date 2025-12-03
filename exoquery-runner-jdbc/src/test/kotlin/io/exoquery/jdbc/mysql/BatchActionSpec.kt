package io.exoquery.jdbc.mysql

import io.exoquery.testdata.Person
import io.exoquery.MySqlDialect
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.jdbc.TestDatabases
import io.exoquery.jdbc.allPeople
import io.exoquery.jdbc.batchDeletePeople
import io.exoquery.jdbc.batchInsertPeople
import io.exoquery.sql
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.jdbc.george
import io.exoquery.jdbc.insertAllPeople
import io.exoquery.jdbc.insertPerson
import io.exoquery.jdbc.joe
import io.exoquery.jdbc.people
import io.exoquery.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class BatchActionSpec : FreeSpec({
  val ctx = TestDatabases.mysql

  @OptIn(TerpalSqlUnsafe::class)
  beforeEach {
    ctx.runActionsUnsafe(
      """
      DELETE FROM Person;
      ALTER TABLE Person AUTO_INCREMENT = 1;
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
      q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "simple with setParams" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { setParams(p) }
      }
      q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "simple with setParams and exclusion" {
      ctx.insertPerson(joe)
      // Modify the ids to make sure it is inserting records with a new Id, not the ones used here
      val insertPeople = batchInsertPeople.map { it.copy(id = it.id + 100) }.asSequence()
      val q = sql.batch(insertPeople) { p ->
        insert<Person> { setParams(p).excluding(id) }
      }
      q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    // Not supported in MySql
    //"with returning" {
    //  ctx.insertPerson(joe)
    //  val q = sql.batch(batchInsertPeople.asSequence()) { p ->
    //    insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returning { p -> p.id + 100 }
    //  }
    //  q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(101, 102, 103)
    //  ctx.people() shouldContainExactlyInAnyOrder allPeople
    //}
    //"with returning and params" {
    //  ctx.insertPerson(joe)
    //  val q = sql.batch(batchInsertPeople.asSequence()) { p ->
    //    insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returning { pp -> pp.id + 100 to param(p.firstName) }
    //  }
    //  q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf((101 to "Joe"), (102 to "Joe"), (103 to "Jim"))
    //  ctx.people() shouldContainExactlyInAnyOrder people + george
    //}
    "with returning keys" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returningKeys { id }
      }
      q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(2, 3, 4)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }
  }

  "update" - {
    val updatedPeople = listOf(Person(1, "Joe-A", "Bloggs", 112), Person(2, "Joe-A", "Doggs", 222), Person(3, "Jim-A", "Roogs", 333))
    val allNewPeople = updatedPeople + george

    "simple" {
      ctx.insertAllPeople()
      val q = sql.batch(updatedPeople.asSequence()) { p ->
        update<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.filter { pp -> pp.id == param(p.id) }
      }
      q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    "simple with setParams and exclusion" {
      ctx.insertAllPeople()
      val peopleWithOddIds = updatedPeople.asSequence().map { it.copy(id = it.id + 100) }
      val q = sql.batch(peopleWithOddIds) { p ->
        update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.lastName == param(p.lastName) }
      }
      q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    // Not supported in MySql
    //"simple with setParams and exclusion and returning param" {
    //  ctx.insertAllPeople()
    //  val peopleWithOddIds = updatedPeople.asSequence().map { it.copy(id = it.id + 100) }
    //  val q = sql.batch(peopleWithOddIds) { p ->
    //    update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.lastName == param(p.lastName) }.returning { pp -> pp.id to param(p.firstName) }
    //  }
    //  q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1 to "Joe-A", 2 to "Joe-A", 3 to "Jim-A")
    //  ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    //}
    // NOTE: Mysql returns an empty list when using returningKeys with update and delete, probably using it should be some sort of warning
    //"returningKeys" {
    //  ctx.insertAllPeople()
    //  val q = sql.batch(updatedPeople.asSequence()) { p ->
    //    update<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.filter { pp -> pp.id == param(p.id) }.returningKeys { id }
    //  }
    //  q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(2, 3, 4)
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
      q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    "using whole object" {
      ctx.insertAllPeople()
      val q = sql.batch(batchDeletePeople.asSequence()) { p ->
        delete<Person>().filter { pp -> pp.id == param(p.id) }
      }
      q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    // Not supported in MySql
    //"with returning" {
    //  ctx.insertAllPeople()
    //  val q = sql.batch(ids) { id ->
    //    delete<Person>().filter { pp -> pp.id == param(id) }.returning { pp -> pp.id }
    //  }
    //  q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    //}

    // NOTE: Mysql returns an empty list when using returningKeys with update and delete, probably using it should be some sort of warning
    //"with returning keys" {
    //  ctx.insertAllPeople()
    //  val q = sql.batch(ids) { pid ->
    //    delete<Person>().filter { pp -> pp.id == param(pid) }.returningKeys { id }
    //  }
    //  q.build<MySqlDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    //}
  }
})
