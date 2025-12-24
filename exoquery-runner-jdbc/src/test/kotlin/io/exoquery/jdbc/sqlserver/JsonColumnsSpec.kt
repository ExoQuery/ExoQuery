package io.exoquery.jdbc.sqlserver

import io.exoquery.*
import io.exoquery.ExoValue
import kotlinx.serialization.Serializable
import io.exoquery.controller.SqlJsonValue
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.jdbc.TestDatabases
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe


class JsonColumnsSpec : FreeSpec({
  val ctx = TestDatabases.sqlServer

  @OptIn(TerpalSqlUnsafe::class)
  beforeEach {
    ctx.runActionsUnsafe(
      """
      DELETE FROM JsonExample;
      INSERT INTO JsonExample (id, value) VALUES (1, '{"name":"Joe", "age":123}');
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
        sql { Table<JsonExample>() }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123))
        )
      }
      "select field" {
        sql { Table<JsonExample>().map { it.value } }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
          Person("Joe", 123)
        )
      }
      "insert" {
        val q = sql {
          insert<JsonExample> { set(id to 2, value to param(jim)) }
        }
        q.build<PostgresDialect>().runOn(ctx) shouldBe 1
        sql { Table<JsonExample>() }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123)),
          JsonExample(2, Person("Jim", 456)),
        )
      }
      "insert - paramCustom" {
        val q = sql {
          insert<JsonExample> { set(id to 2, value to paramCustom(jim)) }
        }
        q.build<PostgresDialect>().runOn(ctx) shouldBe 1
        sql { Table<JsonExample>() }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
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
        sql { Table<JsonExample>() }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123)),
          JsonExample(2, Person("Jim", 456)),
        )
      }
    }
  }

  "simple json/jsonb - annotation on member" - {
    @Serializable
    data class Person(val name: String, val age: Int)
    @Serializable
    data class JsonExample(val id: Int, @SqlJsonValue val value: Person)

    val joe = Person("Joe", 123)
    val jim = Person("Jim", 456)

    "json" - {
      "select product" {
        sql { Table<JsonExample>() }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123))
        )
      }
      "select field".config(enabled = false) {
        sql { Table<JsonExample>().map { it.value } }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
          Person("Joe", 123)
        )
      }
      "insert".config(enabled = false) {
        // Need to mark it an ExoValue because `param` function would not otherwise know that it is a value-type
        val insertJim: @ExoValue Person = Person("Jim", 456)
        val q = sql {
          insert<JsonExample> { set(id to 2, value to param(insertJim)) }
        }
        q.build<PostgresDialect>().runOn(ctx) shouldBe 1
        sql { Table<JsonExample>() }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123)),
          JsonExample(2, Person("Jim", 456)),
        )
      }
      "insert - paramCustom".config(enabled = false) {
        val q = sql {
          insert<JsonExample> { set(id to 2, value to paramCustom(jim)) }
        }
        q.build<PostgresDialect>().runOn(ctx) shouldBe 1
        sql { Table<JsonExample>() }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123)),
          JsonExample(2, Person("Jim", 456)),
        )
      }
      "insert - setParams".config(enabled = false) {
        val jsonValue = JsonExample(2, jim)
        val q = sql {
          insert<JsonExample> { setParams(jsonValue) }
        }
        q.build<PostgresDialect>().runOn(ctx) shouldBe 1
        sql { Table<JsonExample>() }.buildFor.SqlServer().runOn(ctx) shouldBe listOf(
          JsonExample(1, Person("Joe", 123)),
          JsonExample(2, Person("Jim", 456)),
        )
      }
    }
  }
})
