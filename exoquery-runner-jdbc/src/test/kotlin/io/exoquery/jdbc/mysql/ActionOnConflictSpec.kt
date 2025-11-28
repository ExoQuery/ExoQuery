package io.exoquery.jdbc.mysql

import io.exoquery.testdata.Person
import io.exoquery.jdbc.TestDatabases
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.jdbc.insertPerson
import io.exoquery.jdbc.joe
import io.exoquery.jdbc.people
import io.exoquery.runOn
import io.exoquery.MySqlDialect
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ActionOnConflictSpec : FreeSpec({
  val ctx = TestDatabases.mysql

  beforeEach {
    ctx.runActions(
      """
     DELETE FROM Person;
      ALTER TABLE Person AUTO_INCREMENT = 1;
      DELETE FROM Address;
      DELETE FROM Robot;
      """
    )
  }

  "onConflictUpdate" - {
    "simple" {
      ctx.insertPerson(joe)
      val nameVar = "Joee"
      val lastNameVar = "Bloggsee"
      val q = sql {
        insert<Person> {
          set(id to param(joe.id), firstName to param(nameVar), lastName to param(lastNameVar), age to 1234)
            .onConflictUpdate(id) { excluding -> set(firstName to firstName + "_" + excluding.firstName, lastName to lastName + "_" + excluding.lastName, age to excluding.age) }
        }
      }
      q.build<MySqlDialect>().runOn(ctx) shouldBe 2
      ctx.people() shouldBe listOf(Person(1, "Joe_Joee", "Bloggs_Bloggsee", 1234))
    }
    // MySql does not support returning clauses
    //"simple with returning" {
    //  ctx.insertPerson(joe)
    //  val nameVar = "Joee"
    //  val lastNameVar = "Bloggsee"
    //  val q = sql {
    //    insert<Person> {
    //      set(id to param(joe.id), firstName to param(nameVar), lastName to param(lastNameVar), age to 1234)
    //        .onConflictUpdate(id) { excluding -> set(firstName to firstName + "_" + excluding.firstName, lastName to lastName + "_" + excluding.lastName, age to excluding.age) }
    //    }.returning { p -> p.id + 100 }
    //  }
    //  q.build<MySqlDialect>().runOn(ctx) shouldBe 101
    //  ctx.people() shouldBe listOf(Person(1, "Joe_Joee", "Bloggs_Bloggsee", 1234))
    //}
  }
  "onConflictIgnore" - {
    "simple" {
      ctx.insertPerson(joe)
      val nameVar = "Joee"
      val lastNameVar = "Bloggsee"
      val q = sql {
        insert<Person> {
          set(id to param(joe.id), firstName to param(nameVar), lastName to param(lastNameVar), age to 1234)
            .onConflictIgnore()
        }
      }
      q.build<MySqlDialect>().runOn(ctx) shouldBe 0
      ctx.people() shouldBe listOf(Person(1, "Joe", "Bloggs", 111))
    }
  }
})
