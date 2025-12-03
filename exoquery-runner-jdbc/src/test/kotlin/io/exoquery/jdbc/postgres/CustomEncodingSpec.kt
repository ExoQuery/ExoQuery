package io.exoquery.jdbc.postgres

import io.exoquery.jdbc.TestDatabases
import io.exoquery.annotation.ExoField
import io.exoquery.annotation.ExoValue
import io.exoquery.sql
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.jdbc.runOn
import io.exoquery.PostgresDialect
import io.exoquery.controller.TerpalSqlUnsafe
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind


data class FirstName(val name: String)

object FirstNameSerializer: KSerializer<FirstName> {
  override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("FirstName", PrimitiveKind.STRING)
  override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder) =
    FirstName(decoder.decodeString())
  override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: FirstName) =
    encoder.encodeString(value.name)
}

@ExoValue
data class FirstNameAnnotated(val name: String)

object FirstNameAnnotatedSerializer: KSerializer<FirstNameAnnotated> {
  override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("FirstNameAnnotated", PrimitiveKind.STRING)
  override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder) =
    FirstNameAnnotated(decoder.decodeString())
  override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: FirstNameAnnotated) =
    encoder.encodeString(value.name)
}

class CustomEncodingSpec : FreeSpec({
  val ctx = TestDatabases.postgres

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

  "insert" - {
    "simple with custom encoding - ExoValue" {
      @Serializable
      data class Person(
        val id: Long,
        @ExoValue
        @Serializable(with = FirstNameSerializer::class)
        val firstName: FirstName,
        val lastName: String,
        val age: Int
      )

      val insertPeople = listOf(
        Person(1, FirstName("Joe"), "Doe", 30),
        Person(2, FirstName("Jane"), "Doe", 25),
        Person(3, FirstName("John"), "Smith", 40)
      )

      val q = sql.batch(insertPeople.asSequence()) { p ->
        insert<Person> { setParams(p) }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)

      val retrievePeople =
        sql {
          Table<Person>()
        }
      retrievePeople.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder insertPeople
    }
    "simple with custom encoding - ExoField" {
      @Serializable
      data class Person(
        val id: Long,
        @ExoField("firstname")
        @Serializable(with = FirstNameSerializer::class)
        val firstName: FirstName,
        val lastName: String,
        val age: Int
      )

      val insertPeople = listOf(
        Person(1, FirstName("Joe"), "Doe", 30),
        Person(2, FirstName("Jane"), "Doe", 25),
        Person(3, FirstName("John"), "Smith", 40)
      )

      val q = sql.batch(insertPeople.asSequence()) { p ->
        insert<Person> { setParams(p) }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)

      val retrievePeople =
        sql {
          Table<Person>()
        }
      retrievePeople.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder insertPeople
    }

    "simple with custom encoding - ExoValue on type" {
      @Serializable
      data class Person(
        val id: Long,
        @Serializable(with = FirstNameAnnotatedSerializer::class)
        val firstName: FirstNameAnnotated,
        val lastName: String,
        val age: Int
      )

      val insertPeople = listOf(
        Person(1, FirstNameAnnotated("Joe"), "Doe", 30),
        Person(2, FirstNameAnnotated("Jane"), "Doe", 25),
        Person(3, FirstNameAnnotated("John"), "Smith", 40)
      )

      val q = sql.batch(insertPeople.asSequence()) { p ->
        insert<Person> { setParams(p) }
      }
      q.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(1, 1, 1)

      val retrievePeople =
        sql {
          Table<Person>()
        }
      retrievePeople.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder insertPeople
    }
  }
})
