package io.exoquery.jdbc.postgres

import io.exoquery.jdbc.TestDatabases
import io.exoquery.sql
import io.exoquery.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.bigdecimal.shouldBeEqualIgnoringScale
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

class CastingSpec : FreeSpec({
  val ctx = TestDatabases.postgres

  "Int to String" {
    val q = sql.select { 1.toString() }
    q.buildFor.Postgres().runOn(ctx).first() shouldBe "1"
  }

  @Serializable
  data class Wrapper(@Contextual val value: BigDecimal)

  "Int to BigDecimal" {
    val q = sql.select { Wrapper(1.toBigDecimal()) }
    q.buildFor.Postgres().runOn(ctx).first().value shouldBeEqualIgnoringScale BigDecimal(1)
  }
  "Float to BigDecimal" {
    val q = sql.select { Wrapper(1.1f.toBigDecimal()) }
    q.buildFor.Postgres().runOn(ctx).first().value shouldBeEqualIgnoringScale BigDecimal("1.1")
  }
  "Double to BigDecimal" {
    val q = sql.select { Wrapper(1.1.toBigDecimal()) }
    q.buildFor.Postgres().runOn(ctx).first().value shouldBeEqualIgnoringScale BigDecimal("1.1")
  }

  "String to Long" {
    val q = sql.select { "1".toLong() }
    q.buildFor.Postgres().runOn(ctx).first() shouldBe 1
  }
  "String to Int" {
    val q = sql.select { "1".toInt() }
    q.buildFor.Postgres().runOn(ctx).first() shouldBe 1
  }
  "String to Short" {
    val q = sql.select { "1".toShort() }
    q.buildFor.Postgres().runOn(ctx).first() shouldBe 1
  }
  "String to Double" {
    val q = sql.select { "1.2".toDouble() }
    q.buildFor.Postgres().runOn(ctx).first() shouldBe (1.2).toDouble()
  }
  "String to Float" {
    val q = sql.select { "1.2".toFloat() }
    q.buildFor.Postgres().runOn(ctx).first() shouldBe (1.2).toFloat()
  }
  "String to Boolean" {
    val q = sql.select { "true".toBoolean() }
    q.buildFor.Postgres().runOn(ctx).first() shouldBe true
  }
})
