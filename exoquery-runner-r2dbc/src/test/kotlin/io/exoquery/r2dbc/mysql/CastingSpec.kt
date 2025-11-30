package io.exoquery.r2dbc.mysql

import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.sql
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class CastingSpec : FreeSpec({
  val ctx = R2dbcControllers.Mysql(connectionFactory = TestDatabasesR2dbc.mysql)

  "Int to String" {
    val q = sql.select { 1.toString() }
    q.buildFor.MySql().runOn(ctx).first() shouldBe "1"
  }

  "String to Long" {
    val q = sql.select { "1".toLong() }
    q.buildFor.MySql().runOn(ctx).first() shouldBe 1
  }
  "String to Int" {
    val q = sql.select { "1".toInt() }
    q.buildFor.MySql().runOn(ctx).first() shouldBe 1
  }
  "String to Short" {
    val q = sql.select { "1".toShort() }
    q.buildFor.MySql().runOn(ctx).first() shouldBe 1
  }
  "String to Double" {
    val q = sql.select { "1.2".toDouble() }
    q.buildFor.MySql().runOn(ctx).first() shouldBe (1.2).toDouble()
  }
  "String to Float" {
    val q = sql.select { "1.2".toFloat() }
    q.buildFor.MySql().runOn(ctx).first() shouldBe (1.2).toFloat()
  }
  // Due to boolean vendorization this ends up being:
  // SELECT CASE WHEN 'true' = 'true' THEN 1 ELSE 0 END AS value
  // My the MySQL R2dbc driver does not like to cast a INT (i.e. 1 or 0) to a boolean
  //"String to Boolean" {
  //  val q = sql.select { "true".toBoolean() }
  //  q.buildFor.MySql().runOn(ctx).first() shouldBe true
  //}
})
