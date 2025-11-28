package io.exoquery.r2dbc.h2

import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.sql
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class CastingSpec : FreeSpec({
  val ctx = R2dbcControllers.H2(connectionFactory = TestDatabasesR2dbc.h2)

  "Int to String" {
    val q = sql.select { 1.toString() }
    q.buildFor.H2().runOn(ctx).first() shouldBe "1"
  }

  "String to Long" {
    val q = sql.select { "1".toLong() }
    q.buildFor.H2().runOn(ctx).first() shouldBe 1
  }
  "String to Int" {
    val q = sql.select { "1".toInt() }
    q.buildFor.H2().runOn(ctx).first() shouldBe 1
  }
  "String to Short" {
    val q = sql.select { "1".toShort() }
    q.buildFor.H2().runOn(ctx).first() shouldBe 1
  }
  "String to Double" {
    val q = sql.select { "1.2".toDouble() }
    q.buildFor.H2().runOn(ctx).first() shouldBe (1.2).toDouble()
  }
  "String to Float" {
    val q = sql.select { "1.2".toFloat() }
    q.buildFor.H2().runOn(ctx).first() shouldBe (1.2).toFloat()
  }
  "String to Boolean" {
    val q = sql.select { "true".toBoolean() }
    q.buildFor.H2().runOn(ctx).first() shouldBe true
  }
})
