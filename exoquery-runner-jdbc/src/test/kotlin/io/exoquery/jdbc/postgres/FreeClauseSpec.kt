package io.exoquery.jdbc.postgres

import io.exoquery.testdata.Person
import io.exoquery.SqlAction
import io.exoquery.SqlQuery
import io.exoquery.PostgresDialect
import io.exoquery.jdbc.TestDatabases
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.jdbc.joe
import io.exoquery.jdbc.people
import io.exoquery.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class FreeClauseSpec : FreeSpec({
  val ctx = TestDatabases.postgres

  beforeEach {
    ctx.runActions(
      """
      TRUNCATE TABLE Person RESTART IDENTITY CASCADE;
      TRUNCATE TABLE Address RESTART IDENTITY CASCADE;
      TRUNCATE TABLE Robot RESTART IDENTITY CASCADE;
      """
    )
  }

  "whole-body insert" {
    val q = sql {
      free("INSERT INTO Person (firstName, lastName, age) VALUES ('Joe', 'Bloggs', 111)")
        .asPure<SqlAction<Nothing, Long>>()
      //insert<Person> { set(firstName to "Joe", lastName to "Bloggs", age to 111) }
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldBe listOf(joe)
  }

  "whole-body select" {
    ctx.runActions(
      """
      INSERT INTO Person (firstName, lastName, age) VALUES ('Joe', 'Bloggs', 111)
      """
    )
    val q = sql {
      free("SELECT * FROM Person WHERE firstName = 'Joe'")
        .asPure<SqlQuery<Person>>()
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe listOf(joe)
    ctx.people() shouldContainExactlyInAnyOrder listOf(joe)
  }
})
