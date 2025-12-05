package io.exoquery.jdbc.mysql

import io.exoquery.*
import kotlinx.serialization.Serializable
import io.exoquery.controller.SqlJsonValue
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.jdbc.TestDatabases
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

@OptIn(TerpalSqlUnsafe::class)
class JsonOpSpec : FreeSpec({
  val ctx = TestDatabases.mysql

  "one level nesting" - {
    @SqlJsonValue
    @Serializable
    data class Person(val name: String, val age: Int)
    @Serializable
    data class JsonExample(val id: Int, val value: Person)

    val joe = Person("Joe", 123)
    val jim = Person("Jim", 456)

    ctx.runActionsUnsafe(
      """
      DELETE FROM JsonExample;
      INSERT INTO JsonExample (id, value) VALUES (1, '{"name":"Joe", "age":123}'), (2, '{"name":"Jim", "age":456}');
      """.trimIndent()
    )

    "select string field" {
      val ret = sql { Table<JsonExample>().map { it.value.name } }.buildFor.MySql().runOn(ctx)
      ret shouldContainExactlyInAnyOrder listOf("Joe", "Jim")
    }
    "select int field" {
      val ret = sql { Table<JsonExample>().map { it.value.age } }.buildFor.MySql().runOn(ctx)
      ret shouldContainExactlyInAnyOrder listOf(123, 456)
    }
    "filter by int field" {
      val ret = sql { Table<JsonExample>().filter { it.value.age > 200 } }.buildFor.MySql().runOn(ctx)
      ret shouldContainExactlyInAnyOrder listOf(JsonExample(2, jim))
    }
  }

  "two level nesting" - {
    @SqlJsonValue
    @Serializable
    data class Address(val street: String, val number: Int)
    @SqlJsonValue
    @Serializable
    data class PersonWithAddress(val name: String, val address: Address)
    @Serializable
    data class JsonExample(val id: Int, val value: PersonWithAddress)

    val joe = PersonWithAddress("Joe", Address("Main St", 123))
    val jim = PersonWithAddress("Jim", Address("Second St", 456))

    ctx.runActionsUnsafe(
      """
      DELETE FROM JsonExample;
      INSERT INTO JsonExample (id, value) VALUES
        (1, '{"name":"Joe", "address": {"street":"Main St", "number":123}}'),
        (2, '{"name":"Jim", "address": {"street":"Second St", "number":456}}');
      """.trimIndent()
    )

    "select nested string field" {
      val ret = sql { Table<JsonExample>().map { it.value.address.street } }.buildFor.MySql().runOn(ctx)
      ret shouldContainExactlyInAnyOrder listOf("Main St", "Second St")
    }
    "select nested int field" {
      val ret = sql { Table<JsonExample>().map { it.value.address.number } }.buildFor.MySql().runOn(ctx)
      ret shouldContainExactlyInAnyOrder listOf(123, 456)
    }
    "filter by nested int field" {
      val ret = sql { Table<JsonExample>().filter { it.value.address.number > 200 } }.buildFor.MySql().runOn(ctx)
      ret shouldContainExactlyInAnyOrder listOf(JsonExample(2, jim))
    }
  }

  "three level nesting" - {
    @SqlJsonValue
    @Serializable
    data class Country(val name: String, val code: String)
    @SqlJsonValue
    @Serializable
    data class AddressWithCountry(val street: String, val number: Int, val country: Country)
    @SqlJsonValue
    @Serializable
    data class PersonWithAddressAndCountry(val name: String, val address: AddressWithCountry)
    @Serializable
    data class JsonExample(val id: Int, val value: PersonWithAddressAndCountry)
    val joe = PersonWithAddressAndCountry("Joe", AddressWithCountry("Main St", 123, Country("USA", "US")))
    val jim = PersonWithAddressAndCountry("Jim", AddressWithCountry("Second St", 456, Country("Canada", "CA")))

    ctx.runActionsUnsafe(
      """
      DELETE FROM JsonExample;
      INSERT INTO JsonExample (id, value) VALUES
        (1, '{"name":"Joe", "address": {"street":"Main St", "number":123, "country": {"name":"USA", "code":"US"}}}'),
        (2, '{"name":"Jim", "address": {"street":"Second St", "number":456, "country": {"name":"Canada", "code":"CA"}}}');
      """.trimIndent()
    )
    "select three level nested string field" {
      val ret = sql { Table<JsonExample>().map { it.value.address.country.name } }.buildFor.MySql().runOn(ctx)
      ret shouldContainExactlyInAnyOrder listOf("USA", "Canada")
    }
    "select three level nested string field 2" {
      val ret = sql { Table<JsonExample>().map { it.value.address.country.code } }.buildFor.MySql().runOn(ctx)
      ret shouldContainExactlyInAnyOrder listOf("US", "CA")
    }
    "filter by three level nested string field" {
      val ret = sql { Table<JsonExample>().filter { it.value.address.country.code == "CA" } }.buildFor.MySql().runOn(ctx)
      ret shouldContainExactlyInAnyOrder listOf(JsonExample(2, jim))
    }
  }
})
