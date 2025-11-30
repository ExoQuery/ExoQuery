package io.exoquery.r2dbc.mysql

import io.exoquery.testdata.Person
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.r2dbc.insertPerson
import io.exoquery.r2dbc.joe
import io.exoquery.r2dbc.people
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.MySqlDialect
import io.exoquery.r2dbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ActionOnConflictSpec : FreeSpec({
  val ctx = R2dbcControllers.Mysql(connectionFactory = TestDatabasesR2dbc.mysql)

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
