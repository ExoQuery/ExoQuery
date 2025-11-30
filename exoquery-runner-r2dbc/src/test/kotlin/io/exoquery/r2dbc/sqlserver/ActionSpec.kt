package io.exoquery.r2dbc.sqlserver

import io.exoquery.testdata.Person
import io.exoquery.SqlAction
import io.exoquery.SqlServerDialect
import io.exoquery.controller.r2dbc.R2dbcController
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.r2dbc.joe
import io.exoquery.r2dbc.people
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.r2dbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class ActionSpec : FreeSpec({
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
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "simple with params" {
      val q = sql {
        insert<Person> { set(firstName to param("Joe"), lastName to param("Bloggs"), age to param(111)) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "simple with setParams" {
      val q = sql {
        insert<Person> { setParams(Person(1, "Joe", "Bloggs", 111)) }
      }
      val qq = sql {
        free("SET IDENTITY_INSERT Person ON\n ${q} \nSET IDENTITY_INSERT Person OFF").asPure<SqlAction<Person, Long>>()
      }

      qq.build<SqlServerDialect>().runOn(ctx) //shouldBe 1 // R2dbc driver won't return the generated ID when it is being manually inserted
      ctx.people() shouldBe listOf(joe)
    }
    "simple with setParams and exclusion" {
      val q = sql {
        insert<Person> { setParams(Person(1, "Joe", "Bloggs", 111)).excluding(id) }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
    "with returning" {
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 }
      }
      val build = q.build<SqlServerDialect>()
      build.runOn(ctx) shouldBe 101
      ctx.people() shouldBe listOf(joe)
    }
    "with returning using param" {
      val n = 1000
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id + 100 + param(n) }
      }
      val build = q.build<SqlServerDialect>()
      build.runOn(ctx) shouldBe 1101
      ctx.people() shouldBe listOf(joe)
    }
    "with returning - multiple" {
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returning { p -> p.id to p.firstName }
      }
      val build = q.build<SqlServerDialect>()
      build.runOn(ctx) shouldBe (1 to "Joe")
      ctx.people() shouldBe listOf(joe)
    }
    "with returning keys" {
      val q = sql {
        insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.returningKeys { id }
      }
      val build = q.build<SqlServerDialect>()
      build.runOn(ctx) shouldBe 1
      ctx.people() shouldBe listOf(joe)
    }
  }

  val joe = Person(1, "Joe", "Bloggs", 111)
  val george = Person(1, "George", "Googs", 555)
  val jim = Person(2, "Jim", "Roogs", 222)

  suspend fun R2dbcController.insertGeorgeAndJim() =
    this.runActions(
      """
        SET IDENTITY_INSERT Person ON
        INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'George', 'Googs', 555)
        INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Jim', 'Roogs', 222)
        SET IDENTITY_INSERT Person OFF
      """.trimIndent()
    )

  "update" - {
    "simple" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "no condition" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to param("Joe"), lastName to param("Bloggs"), age to 111) }.all()
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldBe 2
      ctx.people() shouldContainExactlyInAnyOrder listOf(
        Person(1, "Joe", "Bloggs", 111),
        Person(2, "Joe", "Bloggs", 111)
      )
    }
    // NOTE: Cannot update an identity column in SQL Server i.e. will throw:
    //       com.microsoft.sqlserver.jdbc.SQLServerException: Cannot update identity column 'id'.
    //"with setParams" {
    //  ctx.insertGeorgeAndJim()
    //  val updateCall = Person(1, "Joe", "Bloggs", 111)
    //  val q = sql {
    //    update<Person> { setParams(updateCall) }.filter { p -> p.id == 1 }
    //  }
    //  q.build<SqlServerDialect>().runOn(ctx) shouldBe 1
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    //}
    "with setParams and exclusion" {
      ctx.insertGeorgeAndJim()
      val updateCall = Person(1000, "Joe", "Bloggs", 111)
      val q = sql {
        update<Person> { setParams(updateCall).excluding(id) }.filter { p -> p.id == 1 }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "with returning" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returning { p -> p.id + 100 }
      }.determinizeDynamics()
      val build = q.build<SqlServerDialect>()
      build.runOn(ctx) shouldBe 101
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    "with returning - multiple" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returning { p -> p.id to p.firstName }
      }
      val build = q.build<SqlServerDialect>()
      build.runOn(ctx) shouldBe (1 to "Joe")
      ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    }
    // Not supported in SqlServer
    //"with returningKeys" {
    //  ctx.insertGeorgeAndJim()
    //  val q = sql {
    //    update<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }.filter { p -> p.id == 1 }.returningKeys { id }
    //  }
    //  val build = q.build<SqlServerDialect>()
    //  build.runOn(ctx) shouldBe 1
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(joe, jim)
    //}
  }

  "delete" - {
    "simple" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        delete<Person>().filter { p -> p.id == 1 }
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldBe 1
      ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
    }
    "no condition" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        delete<Person>().all()
      }
      q.build<SqlServerDialect>().runOn(ctx) shouldBe 2
      ctx.people() shouldBe emptyList()
    }
    "with returning" {
      ctx.insertGeorgeAndJim()
      val q = sql {
        delete<Person>().filter { p -> p.id == 1 }.returning { p -> p.id + 100 }
      }
      val build = q.build<SqlServerDialect>()
      build.runOn(ctx) shouldBe 101
      ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
    }
    // Not supported in SqlServer
    //"with returningKeys" {
    //  ctx.insertGeorgeAndJim()
    //  val q = sql {
    //    delete<Person>().filter { p -> p.id == 1 }.returningKeys { id }
    //  }
    //  val build = q.build<SqlServerDialect>()
    //  build.runOn(ctx) shouldBe 1
    //  ctx.people() shouldContainExactlyInAnyOrder listOf(jim)
    //}
  }
})
