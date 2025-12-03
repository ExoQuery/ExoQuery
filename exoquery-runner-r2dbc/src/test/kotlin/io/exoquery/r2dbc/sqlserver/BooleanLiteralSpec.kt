package io.exoquery.r2dbc.sqlserver

import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.sql
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.runOn
import io.exoquery.testdata.Person
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class BooleanLiteralSpec : FreeSpec({
  val ctx = R2dbcControllers.SqlServer(connectionFactory = TestDatabasesR2dbc.sqlServer)

  @OptIn(TerpalSqlUnsafe::class)
  beforeSpec {
    ctx.runActionsUnsafe(
      // Note when doing 'SET IDENTITY_INSERT Person ON INSERT INTO Person' DONT put ';' because then the statement will be executed in a different run and the setting will not take
      """
      DELETE FROM Person;
      SET IDENTITY_INSERT Person ON INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'Joe', 'Bloggs', 111) SET IDENTITY_INSERT Person OFF;
      """
    )
  }

  "where - simple" {
    val q = sql.select {
      val p = from(Table<Person>())
      where { param(true) }
      p.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).first() shouldBe "Joe"
  }

  "where - combined" {
    val q = sql.select {
      val e = from(Table<Person>())
      where { param(true) || e.firstName == "Joe" }
      e.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).first() shouldBe "Joe"
  }

  "where - combined complex A" {
    val q = sql.select {
      val e = from(Table<Person>())
      where { param(false) || if (param(false)) param(false) || param(false) else e.firstName == "Joe" }
      e.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).first() shouldBe "Joe"
  }

  "where - combined complex A - false" {
    val q = sql.select {
      val e = from(Table<Person>())
      where { param(false) || if (param(false)) param(false) || param(false) else e.firstName == "Jack" }
      e.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).isEmpty() shouldBe true
  }

  "where - combined complex C" {
    val q = sql.select {
      val e = from(Table<Person>())
      where { param(false) || if (param(true)) param(false) || param(true) else e.firstName == "Jack" }
      e.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).first() shouldBe "Joe"
  }

  "exists - lifted complex - notnull->false->true" {
    val nullableBool: Boolean? = true
    val q = sql.select {
      val e = from(Table<Person>())
      where { param(nullableBool)?.let { if (param(false)) param(false) else param(true) } ?: false }
      e.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).first() shouldBe "Joe"
  }

  "exists - lifted complex - notnull->true->true" {
    val nullableBool: Boolean? = true
    val q = sql.select {
      val e = from(Table<Person>())
      where { param(nullableBool)?.let { if (param(true)) param(true) else param(false) } ?: false }
      e.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).first() shouldBe "Joe"
  }

  "exists - lifted complex - notnull->true->false" {
    val nullableBool: Boolean? = true
    val q = sql.select {
      val e = from(Table<Person>())
      where { param(nullableBool)?.let { if (param(true)) param(false) else param(true) } ?: false }
      e.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).isEmpty() shouldBe true
  }

  "exists - lifted complex - null->true" {
    val nullableBool: Boolean? = null
    val q = sql.select {
      val e = from(Table<Person>())
      where { param(nullableBool)?.let { if (param(false)) param(false) else param(false) } ?: true }
      e.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).first() shouldBe "Joe"
  }

  "exists - lifted complex - null->false" {
    val nullableBool: Boolean? = null
    val q = sql.select {
      val e = from(Table<Person>())
      where { param(nullableBool)?.let { if (param(true)) param(true) else param(true) } ?: false }
      e.firstName
    }
    q.buildFor.SqlServer().runOn(ctx).isEmpty() shouldBe true
  }
})
