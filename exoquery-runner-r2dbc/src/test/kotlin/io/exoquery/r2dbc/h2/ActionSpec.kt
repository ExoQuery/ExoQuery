package io.exoquery.r2dbc.h2

import io.exoquery.testdata.Person
import io.exoquery.H2Dialect
import io.exoquery.controller.r2dbc.R2dbcController
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.sql
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.r2dbc.joe
import io.exoquery.r2dbc.people
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.SqlServerDialect
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.r2dbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class ActionSpec : FreeSpec({
  val ctx = R2dbcControllers.H2(connectionFactory = TestDatabasesR2dbc.h2)

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
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }
      }
      q.build<H2Dialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "simple with params" {
      val q = sql {
        insert<Person> { set(firstName to param("Joe"), lastName to param("Bloggs"), age to param(111)) }
      }
      q.build<H2Dialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "simple with setParams" {
      val q = sql {
        insert<Person> { setParams(Person(1, "Joe", "Bloggs", 111)) }
      }
      q.build<H2Dialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "simple with setParams and exclusion" {
      val q = sql {
        insert<Person> { setParams(Person(1, "Joe", "Bloggs", 111)).excluding(id) }
      }
      q.build<H2Dialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    // 'RETURNING' clauses in SQL are not supported in H2
    //"with returning" {
    //  val q = sql {
    //    insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 }
    //  }
    //  val build = q.build<H2Dialect>()
    //  build.runOn(ctx) shouldBe 101
    //  ctx.people() shouldBe listOf(joe)
    //}
    //"with returning using param" {
    //  val n = 1000
    //  val q = sql {
    //    insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 + param(n) }
    //  }
    //  val build = q.build<H2Dialect>()
    //  build.runOn(ctx) shouldBe 1101
    //  ctx.people() shouldBe listOf(joe)
    //}
    //"with returning - multiple" {
    //  val q = sql {
    //    insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id to p.firstName }
    //  }
    //  val build = q.build<H2Dialect>()
    //  build.runOn(ctx) shouldBe (1 to "Joe")
    //  ctx.people() shouldBe listOf(joe)
    //}
    "with returning keys" {
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returningKeys { id }
      }
      val build = q.build<H2Dialect>()
      build.runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "with returning keys - multiple" {
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returningKeys { id to firstName }
      }
      val build = q.build<SqlServerDialect>()
      build.runOn(ctx) shouldBe (1 to "Joe")
      ctx.people() shouldBe listOf(joe)
    }
  }

  val joe = Person(1, "Joe", "Bloggs", 111)
  val george = Person(1, "George", "Googs", 555)
  val jim = Person(2, "Jim", "Roogs", 222)

  @OptIn(TerpalSqlUnsafe::class)
  suspend fun R2dbcController.insertGeorgeAndJim() =
    this.runActionsUnsafe(
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
      q.build<H2Dialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "no condition" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to param("Joe"), lastName to param("Bloggs"), age to 111) }.all()
      }
      q.build<H2Dialect>().runOn(ctx) shouldBe 2
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
      q.build<H2Dialect>().runOn(ctx) shouldBe 1
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
      q.build<H2Dialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    // Not supported in H2
    //"with returning" {
    //  ctx.insertGeorgeAndJim()
    //  val q = sql {
    //    update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returning { p -> p.id + 100 }
    //  }
    //  val build = q.build<H2Dialect>()
    //  build.runOn(ctx) shouldBe 101
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    //}
    // Not supported in H2
    //"with returning - multiple" {
    //  ctx.insertGeorgeAndJim()
    //  val q = sql {
    //    update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returning { p -> p.id to p.firstName }
    //  }
    //  val build = q.build<H2Dialect>()
    //  build.runOn(ctx) shouldBe (1 to "Joe")
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    //}
    "with returningKeys" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returningKeys { id }
      }
      val build = q.build<H2Dialect>()
      build.runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "with returning keys - multiple" {
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returningKeys { id to firstName }
      }
      val build = q.build<SqlServerDialect>()
      build.runOn(ctx) shouldBe (1 to "Joe")
      ctx.people() shouldBe listOf(io.exoquery.r2dbc.joe)
    }
  }

  "delete" - {
    "simple" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        delete<Person>().filter { p -> p.id == 1 }
      }
      q.build<H2Dialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
    }
    "no condition" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        delete<Person>().all()
      }
      q.build<H2Dialect>().runOn(ctx) shouldBe 2
      ctx.people() shouldBe emptyList()
    }
    // Not supported in H2
    //"with returning" {
    //  ctx.insertGeorgeAndJim()
    //  val q = sql {
    //    delete<Person>().filter { p -> p.id == 1 }.returning { p -> p.id + 100 }
    //  }
    //  val build = q.build<H2Dialect>()
    //  build.runOn(ctx) shouldBe 101
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
    //}
    // H2 does not support this
    //"with returningKeys" {
    //  ctx.insertGeorgeAndJim()
    //  val q = sql {
    //    delete<Person>().filter { p -> p.id == 1 }.returningKeys { id }
    //  }
    //  val build = q.build<H2Dialect>()
    //  build.runOn(ctx) shouldBe 1
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
    //}
  }
})
