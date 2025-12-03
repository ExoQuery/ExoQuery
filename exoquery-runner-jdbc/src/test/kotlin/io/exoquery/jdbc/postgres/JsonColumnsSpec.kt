package io.exoquery.jdbc.postgres

import io.exoquery.*
import kotlinx.serialization.Serializable
import io.exoquery.controller.SqlJsonValue
import io.exoquery.controller.runActions
import io.exoquery.jdbc.TestDatabases
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe


class JsonColumnsSpec : FreeSpec({
  val ctx = TestDatabases.postgres

  beforeEach {
    ctx.runActions(
      """
      DELETE FROM JsonExample;
      DELETE FROM JsonbExample;
      DELETE FROM JsonbExample2;
      DELETE FROM JsonbExample3;
      INSERT INTO JsonExample (id, value) VALUES (1, '{"name":"Joe", "age":123}');
      INSERT INTO JsonbExample (id, value) VALUES (1, '{"name":"Joe", "age":123}');
      """.trimIndent()
    )
  }

  "simple json/jsonb" - {
    @SqlJsonValue
    @Serializable
    data class Person(val name: String, val age: Int)
    @Serializable
    data class JsonExample(val id: Int, val value: Person)
    @Serializable
    data class JsonbExample(val id: Int, val value: Person)

    val joe = Person("Joe", 123)
    val jim = Person("Jim", 456)

    "json" - {
      "select product" {
        sql { Table<JsonExample>() }.buildFor.Postgres().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123))
        )
      }
      "select field" {
        sql { Table<JsonExample>().map { it.value } }.buildFor.Postgres().runOn(ctx) shouldBe listOf(
          Person("Joe", 123)
        )
      }
      "insert" {
        val q = sql {
          insert<JsonExample> { set(id to 2, value to param(jim)) }
        }
        q.build<PostgresDialect>().runOn(ctx) shouldBe 1
        sql { Table<JsonExample>() }.buildFor.Postgres().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123)),
          JsonExample(2, Person("Jim", 456)),
        )
      }
      "insert - paramCustom" {
        val q = sql {
          insert<JsonExample> { set(id to 2, value to paramCustom(jim)) }
        }
        q.build<PostgresDialect>().runOn(ctx) shouldBe 1
        sql { Table<JsonExample>() }.buildFor.Postgres().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123)),
          JsonExample(2, Person("Jim", 456)),
        )
      }
      "insert - setParams" {
        val jsonValue = JsonExample(2, jim)
        val q = sql {
          insert<JsonExample> { setParams(jsonValue) }
        }
        q.build<PostgresDialect>().runOn(ctx) shouldBe 1
        sql { Table<JsonExample>() }.buildFor.Postgres().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123)),
          JsonExample(2, Person("Jim", 456)),
        )
      }
    }
    "jsonb" - {
      "select product" {
        sql { Table<JsonbExample>() }.buildFor.Postgres().runOn(ctx) shouldBe listOf(
          JsonbExample(1, Person("Joe", 123))
        )
      }
      "select product with filter+params" {
        sql {
          Table<JsonbExample>().filter { it.value in params(listOf(joe, jim)) }
        }.buildFor.Postgres().runOn(ctx) shouldBe listOf(
          JsonbExample(1, Person("Joe", 123))
        )
      }
      "select field" {
        sql { Table<JsonbExample>().map { it.value } }.buildFor.Postgres().runOn(ctx) shouldBe listOf(
          Person("Joe", 123)
        )
      }
      "insert" {
        val q = sql {
          insert<JsonbExample> { set(id to 2, value to param(jim)) }
        }
        q.build<PostgresDialect>().runOn(ctx) shouldBe 1
        sql { Table<JsonbExample>() }.buildFor.Postgres().runOn(ctx) shouldBe listOf(
          JsonbExample(1, Person("Joe", 123)),
          JsonbExample(2, Person("Jim", 456)),
        )
      }
    }
  }
})
