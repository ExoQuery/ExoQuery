package io.exoquery.jdbc.h2

import io.exoquery.testdata.Person
import io.exoquery.H2Dialect
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.jdbc.TestDatabases
import io.exoquery.jdbc.allPeople
import io.exoquery.jdbc.batchDeletePeople
import io.exoquery.jdbc.batchInsertPeople
import io.exoquery.sql
import io.exoquery.controller.jdbc.JdbcController
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
  val ctx = TestDatabases.h2

  @OptIn(TerpalSqlUnsafe::class)
  beforeEach {
    ctx.runActionsUnsafe(
      """
      DELETE FROM Person;
      ALTER TABLE Person ALTER COLUMN id RESTART WITH 1;
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
      val b = q.build<H2Dialect>()
      println(b)
      b.runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "simple with setParams" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { setParams(p) }
      }
      q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    "simple with setParams and exclusion" {
      ctx.insertPerson(joe)
      // Modify the ids to make sure it is inserting records with a new Id, not the ones used here
      val insertPeople = batchInsertPeople.map { it.copy(id = it.id + 100) }.asSequence()
      val q = sql.batch(insertPeople) { p ->
        insert<Person> { setParams(p).excluding(id) }
      }
      q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allPeople
    }

    // Not supported in H2
    //"with returning" {
    //  ctx.insertPerson(joe)
    //  val q = sql.batch(batchInsertPeople.asSequence()) { p ->
    //    insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returning { p -> p.id + 100 }
    //  }
    //  q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(101, 102, 103)
    //  ctx.people() shouldContainExactlyInAnyOrder allPeople
    //}
    //"with returning and params" {
    //  ctx.insertPerson(joe)
    //  val q = sql.batch(batchInsertPeople.asSequence()) { p ->
    //    insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returning { pp -> pp.id + 100 to param(p.firstName) }
    //  }
    //  q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf((101 to "Joe"), (102 to "Joe"), (103 to "Jim"))
    //  ctx.people() shouldContainExactlyInAnyOrder people + george
    //}
    "with returning keys" {
      ctx.insertPerson(joe)
      val q = sql.batch(batchInsertPeople.asSequence()) { p ->
        insert<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.returningKeys { id }
      }
      q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(2, 3, 4)
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
      q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    "simple with setParams and exclusion" {
      ctx.insertAllPeople()
      val peopleWithOddIds = updatedPeople.asSequence().map { it.copy(id = it.id + 100) }
      val q = sql.batch(peopleWithOddIds) { p ->
        update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.lastName == param(p.lastName) }
      }
      q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }

    // .returning is not supported in H2
    //"simple with setParams and exclusion and returning param" {
    //  ctx.insertAllPeople()
    //  val peopleWithOddIds = updatedPeople.asSequence().map { it.copy(id = it.id + 100) }
    //  val q = sql.batch(peopleWithOddIds) { p ->
    //    update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.lastName == param(p.lastName) }.returning { pp -> pp.id to param(p.firstName) }
    //  }
    //  q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1 to "Joe-A", 2 to "Joe-A", 3 to "Jim-A")
    //  ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    //}

    "returningKeys" {
      ctx.insertAllPeople()
      val q = sql.batch(updatedPeople.asSequence()) { p ->
        update<Person> { set(firstName to param(p.firstName), lastName to param(p.lastName), age to param(p.age)) }.filter { pp -> pp.id == param(p.id) }.returningKeys { id }
      }
      q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
      ctx.people() shouldContainExactlyInAnyOrder allNewPeople
    }
  }


  "delete" - {
    val ids = listOf(1, 2, 3).asSequence()

    "simple" {
      ctx.insertAllPeople()
      val q = sql.batch(ids) { id ->
        delete<Person>().filter { pp -> pp.id == param(id) }
      }
      q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    "using whole object" {
      ctx.insertAllPeople()
      val q = sql.batch(batchDeletePeople.asSequence()) { p ->
        delete<Person>().filter { pp -> pp.id == param(p.id) }
      }
      q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)
      ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    }

    // Neither of these are supported in H2
    //"with returning" {
    //  ctx.insertAllPeople()
    //  val q = sql.batch(ids) { id ->
    //    delete<Person>().filter { pp -> pp.id == param(id) }.returning { pp -> pp.id }
    //  }
    //  q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    //}
    //"with returning keys" {
    //  ctx.insertAllPeople()
    //  val q = sql.batch(ids) { pid ->
    //    delete<Person>().filter { pp -> pp.id == param(pid) }.returningKeys { id }
    //  }
    //  q.build<H2Dialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 2, 3)
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(george)
    //}
  }

  val people = listOf(
    Person(1, "Joe", "Bloggs", 111),
    Person(2, "Joe", "Doggs", 222),
    Person(3, "Jim", "Roogs", 333)
  )
  val allPeople = people + george
  val george = Person(1, "George", "Googs", 555)

  @OptIn(TerpalSqlUnsafe::class)
  suspend fun JdbcController.insertPerson(person: Person) =
    this.runActionsUnsafe(
      """
        INSERT INTO Person (id, firstName, lastName, age) VALUES (${person.id}, '${person.firstName}', '${person.lastName}', ${person.age});
      """.trimIndent()
    )

  @OptIn(TerpalSqlUnsafe::class)
  suspend fun JdbcController.insertAllPeople() =
    this.runActionsUnsafe(
      """
        INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'George', 'Googs', 555);
        INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'Joe', 'Bloggs', 111);
        INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Joe', 'Doggs', 222);
        INSERT INTO Person (id, firstName, lastName, age) VALUES (3, 'Jim', 'Roogs', 333);
      """.trimIndent()
    )
})
