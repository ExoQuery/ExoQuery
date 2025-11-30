package io.exoquery.r2dbc.postgres

import io.exoquery.testdata.Person
import io.exoquery.PostgresDialect
import io.exoquery.controller.ControllerError
import io.exoquery.controller.r2dbc.R2dbcController
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.r2dbc.joe
import io.exoquery.r2dbc.people
import io.exoquery.r2dbc.peopleNullable
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.r2dbc.runOn
import io.exoquery.testdata.PersonNullable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.string.shouldContain
import kotlin.to

class ActionSpec : FreeSpec({
  val ctx = R2dbcControllers.Postgres(connectionFactory = TestDatabasesR2dbc.postgres)

  beforeEach {
    ctx.runActions(
      """
      TRUNCATE TABLE Person RESTART IDENTITY CASCADE;
      TRUNCATE TABLE Address RESTART IDENTITY CASCADE;
      TRUNCATE TABLE Robot RESTART IDENTITY CASCADE;
      """
    )
  }

  "insert" - {
    "simple - nullable" {
      val joeNullable = PersonNullable(1, null, "Bloggs", null)
      val q = sql {
        insert<PersonNullable> { set(firstName to null, lastName to "Bloggs", age to null) }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.peopleNullable() shouldBe listOf(joeNullable)
    }
    "simple with params" {
      val q = sql {
        insert<Person> { set(firstName to param("Joe"), lastName to param("Bloggs"), age to param(111)) }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "simple with params - nullable" {
      val joeNullable = PersonNullable(1, null, "Bloggs", null)
      val nameVar: String? = null
      val ageVar: Int? = null
      val q = sql {
        insert<PersonNullable> { set(firstName to param(nameVar), lastName to param("Bloggs"), age to param(ageVar)) }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.peopleNullable() shouldBe listOf(joeNullable)
    }
    // tests where you have param(null) which kotlin thinks is a java.lang.Void
    // I can support this all the way down to the driver level (i.e. it compiles and everything!)
    // but the Postgres PreparedStatement can't handle it.
    "simple with params - nullable - voidEncoder" {
      val e = shouldThrow<ControllerError> {
        val q = sql {
          insert<PersonNullable> { set(firstName to param(null), lastName to param("Bloggs"), age to param(null)) }
        }
        q.build<PostgresDialect>().runOn(ctx)
      }
      e.stackTraceToString() shouldContain "Unsupported null primitive kind: OBJECT"
    }

    "simple with setParams" {
      val q = sql {
        insert<Person> { setParams(Person(1, "Joe", "Bloggs", 111)) }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "simple with setParams - nullable" {
      val joeNullable = PersonNullable(1, null, "Bloggs", null)
      val q = sql {
        insert<PersonNullable> { setParams(PersonNullable(1, null, "Bloggs", null)) }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.peopleNullable() shouldBe listOf(joeNullable)
    }

    "simple with setParams and exclusion" {
      val q = sql {
        insert<Person> { setParams(Person(1, "Joe", "Bloggs", 111)).excluding(id) }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "with returning" {
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 }
      }
      val build = q.build<PostgresDialect>()
      build.runOn(ctx) shouldBe 101
      ctx.people() shouldBe listOf(joe)
    }
    "with returning using param" {
      val n = 1000
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 + param(n) }
      }
      val build = q.build<PostgresDialect>()
      build.runOn(ctx) shouldBe 1101
      ctx.people() shouldBe listOf(joe)
    }
    "with returning - multiple" {
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id to p.firstName }
      }
      val build = q.build<PostgresDialect>()
      build.runOn(ctx) shouldBe (1 to "Joe")
      ctx.people() shouldBe listOf(joe)
    }
    "with returning keys" {
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returningKeys { id }
      }
      val build = q.build<PostgresDialect>()
      build.runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    // Not valid because firstName is not an inserted value
    //"with returning keys multiple" {
    //  val q = sql {
    //    insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returningKeys { id to firstName }
    //  }
    //  val build = q.build<PostgresDialect>()
    //  build.runOn(ctx) shouldBe (1 to "Joe")
    //  ctx.people() shouldBe listOf(joe)
    //}
  }

  val joe = Person(1, "Joe", "Bloggs", 111)
  val george = Person(1, "George", "Googs", 555)
  val jim = Person(2, "Jim", "Roogs", 222)

  suspend fun R2dbcController.insertGeorgeAndJim() =
    this.runActions(
      """
        INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'George', 'Googs', 555);
        INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Jim', 'Roogs', 222);
      """.trimIndent()
    )

  "update" - {
    "simple" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "no condition" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to param("Joe"), lastName to param("Bloggs"), age to 111) }.all()
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 2
      ctx.people() shouldContainExactlyInAnyOrder listOf(
        Person(1, "Joe", "Bloggs", 111),
        Person(2, "Joe", "Bloggs", 111)
      )
    }
    "with setParams" {
      ctx.insertGeorgeAndJim()
      val updateCall = Person(1, "Joe", "Bloggs", 111)
      val q = sql {
        // TODO need to make a warning when this situation happens, can't have param instances here
        // update<Person> { setParams(Person(1, param("Joe"), param("Bloggs"), 111)) }.filter { p -> p.id == 1 }
        update<Person> { setParams(updateCall) }.filter { p -> p.id == 1 }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "with setParams and exclusion" {
      ctx.insertGeorgeAndJim()
      // Set a large Id that should specifically be excluded from insertion
      val updateCall = Person(1000, "Joe", "Bloggs", 111)
      val q = sql {
        // Set the ID to 0 so we can be sure
        update<Person> { setParams(updateCall).excluding(id) }.filter { p -> p.id == 1 }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "with returning" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returning { p -> p.id + 100 }
      }
      val build = q.build<PostgresDialect>()
      build.runOn(ctx) shouldBe 101
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "with returning - multiple" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returning { p -> p.id to p.firstName }
      }
      val build = q.build<PostgresDialect>()
      build.runOn(ctx) shouldBe (1 to "Joe")
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "with returningKeys" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returningKeys { id }
      }
      val build = q.build<PostgresDialect>()
      build.runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
  }

  "delete" - {
    "simple" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        delete<Person>().filter { p -> p.id == 1 }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
    }
    "no condition" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        delete<Person>().all()
      }
      q.build<PostgresDialect>().runOn(ctx) shouldBe 2
      ctx.people() shouldBe emptyList()
    }
    "with returning" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        delete<Person>().filter { p -> p.id == 1 }.returning { p -> p.id + 100 }
      }
      val build = q.build<PostgresDialect>()
      build.runOn(ctx) shouldBe 101
      ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
    }
    "with returningKeys" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        delete<Person>().filter { p -> p.id == 1 }.returningKeys { id }
      }
      val build = q.build<PostgresDialect>()
      build.runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
    }
  }
})
