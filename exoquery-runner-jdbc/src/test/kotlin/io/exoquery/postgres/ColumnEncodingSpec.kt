package io.exoquery.postgres

import io.exoquery.testdata.PersonId
import io.exoquery.testdata.PersonWithIdCtx
import io.exoquery.PostgresDialect
import io.exoquery.TestDatabases
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.jdbc.runOn
import io.exoquery.peopleWithId
import io.exoquery.peopleWithIdCtx
import io.exoquery.testdata.AddressWithId
import io.exoquery.testdata.AddressWithIdCtx
import io.exoquery.testdata.PersonWithId
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class ColumnEncodingSpec : FreeSpec({
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

  "simple - contextual param" {
    val joeWithId = PersonWithIdCtx(PersonId(1), "Joe", "Bloggs", 123)
    val q = sql {
      insert<PersonWithIdCtx> { set(id to paramCtx(joeWithId.id), firstName to param(joeWithId.firstName), lastName to "Bloggs", age to 123) }
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe 1
    ctx.peopleWithIdCtx() shouldBe listOf(joeWithId)
  }

  "setParams - contextual param" {
    val joeWithId = PersonWithIdCtx(PersonId(1), "Joe", "Bloggs", 123)
    val q = sql {
      insert<PersonWithIdCtx> { setParams(joeWithId) }
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe 1
    ctx.peopleWithIdCtx() shouldBe listOf(joeWithId)
  }

  "simple - param" {
    val joeWithId = PersonWithId(PersonId(1), "Joe", "Bloggs", 123)
    val q = sql {
      insert<PersonWithId> { set(id to param(joeWithId.id), firstName to param(joeWithId.firstName), lastName to "Bloggs", age to 123) }
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe 1
    ctx.peopleWithId() shouldBe listOf(joeWithId)
  }

  "setParams - param with id" {
    val joeWithId = PersonWithId(PersonId(1), "Joe", "Bloggs", 123)
    val q = sql {
      insert<PersonWithId> { setParams(joeWithId) }
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe 1
    ctx.peopleWithId() shouldBe listOf(joeWithId)
  }

  // Test upstream fix to terpal-sql: 789512e
  // https://github.com/ExoQuery/terpal-sql/commit/789512ec6d21970c9d1c4909bb9439b20f47130f
  "null join decode - contextual" {
    // NOTE:
    // CANNOT DO .build<PostgresDialect>().runOn(ctx) directly on a sql.batch or it will cause the following error
    // The root cause java.lang.ClassCastException was thrown at: unknown
    //   at org.jetbrains.kotlin.backend.common.CodegenUtil.reportBackendException(CodegenUtil.kt:107)
    //   at org.jetbrains.kotlin.backend.common.CodegenUtil.reportBackendException$default(CodegenUtil.kt:90)
    // TODO file an issue for this

    val joe = PersonWithIdCtx(PersonId(1), "Joe", "Bloggs", 123)
    val jim = PersonWithIdCtx(PersonId(2), "Jim", "Roogs", 222)
    val a1 = AddressWithIdCtx(PersonId(1), "123 Main St", "12345")
    val people = listOf(joe, jim)
    val addresses = listOf(a1)

    val insertPeople = sql.batch(people.asSequence()) { p ->
      insert<PersonWithIdCtx> { setParams(p) }
    }
    insertPeople.build<PostgresDialect>().runOn(ctx) shouldBe listOf(1, 1)

    val insertAddresses = sql.batch(addresses.asSequence()) { a ->
      insert<AddressWithIdCtx> { setParams(a) }
    }
    insertAddresses.build<PostgresDialect>().runOn(ctx) shouldBe listOf(1)

    val q = sql.select {
      val p = from(Table<PersonWithIdCtx>())
      val a = joinLeft(Table<AddressWithIdCtx>()) { a -> p.id == a.ownerId }
      val aa = joinLeft(Table<AddressWithIdCtx>()) { aa -> param(PersonId(1)) == aa.ownerId }
      Triple(p, a, aa)
    }.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Triple(joe, a1, a1),
      Triple(jim, null, a1)
    )
  }

  // Test upstream fix to terpal-sql: 789512e
  // https://github.com/ExoQuery/terpal-sql/commit/789512ec6d21970c9d1c4909bb9439b20f47130f
  "null join decode" {
    val joe = PersonWithId(PersonId(1), "Joe", "Bloggs", 123)
    val jim = PersonWithId(PersonId(2), "Jim", "Roogs", 222)
    val a1 = AddressWithId(PersonId(1), "123 Main St", "12345")
    val people = listOf(joe, jim)
    val addresses = listOf(a1)

    val insertPeople = sql.batch(people.asSequence()) { p ->
      insert<PersonWithId> { setParams(p).excluding(id) }
    }
    insertPeople.build<PostgresDialect>().runOn(ctx) shouldBe listOf(1, 1)

    val insertAddresses = sql.batch(addresses.asSequence()) { a ->
      insert<AddressWithId> { setParams(a) }
    }
    insertAddresses.build<PostgresDialect>().runOn(ctx) shouldBe listOf(1)

    val q = sql.select {
      val p = from(Table<PersonWithId>())
      val a = joinLeft(Table<AddressWithId>()) { a -> p.id == a.ownerId }
      val aa = joinLeft(Table<AddressWithId>()) { aa -> param(PersonId(1)) == aa.ownerId }
      Triple(p, a, aa)
    }.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Triple(joe, a1, a1),
      Triple(jim, null, a1)
    )
  }
})
