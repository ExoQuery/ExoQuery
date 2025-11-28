package io.exoquery.jdbc.postgres

import io.exoquery.testdata.Person
import io.exoquery.PostgresDialect
import io.exoquery.SqlExpression
import io.exoquery.jdbc.TestDatabases
import io.exoquery.annotation.SqlDynamic
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.jdbc.george
import io.exoquery.jdbc.insertAllPeople
import io.exoquery.jdbc.people
import io.exoquery.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class DynamicActionSpec : FreeSpec({
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

  "should be able to construct a dynamic clause using parameterization" {
    val expectedPeople = people.map { p -> p.copy(firstName = p.firstName + "-A") } + george
    val namesToModify = people.map { it.lastName }

    @SqlDynamic
    fun conditionClause(p: SqlExpression<Person>) =
      namesToModify
        .map { n -> sql.expression { p.use.lastName == param(n) } }
        .reduce { a, b -> sql.expression { a.use || b.use } }

    ctx.insertAllPeople()
    val update =
      sql {
        update<Person> {
          set(firstName to firstName + "-A")
        }.filter { p ->
          conditionClause(sql.expression { p }).use
        }
      }

    val compiledAction = update.build<PostgresDialect>()
    compiledAction.value shouldBe "UPDATE Person SET firstName = (firstName || '-A') WHERE lastName = ? OR lastName = ? OR lastName = ?"
    compiledAction.runOn(ctx) shouldBe 3L
    ctx.people() shouldContainExactlyInAnyOrder expectedPeople
  }
})
