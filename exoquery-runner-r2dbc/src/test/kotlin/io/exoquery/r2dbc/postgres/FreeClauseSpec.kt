package io.exoquery.r2dbc.postgres

import io.exoquery.testdata.Person
import io.exoquery.SqlAction
import io.exoquery.SqlQuery
import io.exoquery.PostgresDialect
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.sql
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.r2dbc.joe
import io.exoquery.r2dbc.people
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.r2dbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class FreeClauseSpec : FreeSpec({
  val ctx = R2dbcControllers.Postgres(connectionFactory = TestDatabasesR2dbc.postgres)

  @OptIn(TerpalSqlUnsafe::class)
  beforeEach {
    ctx.runActionsUnsafe(
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
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe 1
    ctx.people() shouldBe listOf(joe)
  }

  @OptIn(TerpalSqlUnsafe::class)
  "whole-body select" {
    ctx.runActionsUnsafe(
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
