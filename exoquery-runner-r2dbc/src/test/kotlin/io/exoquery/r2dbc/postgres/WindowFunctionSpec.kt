package io.exoquery.r2dbc.postgres

import io.exoquery.testdata.Person
import io.exoquery.PostgresDialect
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class WindowFunctionSpec : FreeSpec({
  val ctx = R2dbcControllers.Postgres(connectionFactory = TestDatabasesR2dbc.postgres)

  beforeSpec {
    ctx.runActions(
      """
      TRUNCATE TABLE Person RESTART IDENTITY CASCADE;
      DELETE FROM Address;
      INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'Joe', 'Bloggs', 111);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Joe', 'Doggs', 222);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (3, 'Jim', 'Roogs', 333);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (4, 'Jim', 'Smith', 444);
      INSERT INTO Address (ownerId, street, zip) VALUES (1, '123 Main St', '12345');
      INSERT INTO Address (ownerId, street, zip) VALUES (1, '456 Elm St', '67890');
      INSERT INTO Address (ownerId, street, zip) VALUES (2, '789 Oak St', '54321');
      INSERT INTO Address (ownerId, street, zip) VALUES (3, '101 Pine St', '98765');
      """
    )
  }

  "rank over partition by firstName order by lastName" {
    val q = sql.select {
      val p = from(Table<Person>())
      p to over().partitionBy(p.firstName).sortBy(p.lastName).rank()
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe listOf(
      Person(3, "Jim", "Roogs", 333) to 1,
      Person(4, "Jim", "Smith", 444) to 2,
      Person(1, "Joe", "Bloggs", 111) to 1,
      Person(2, "Joe", "Doggs", 222) to 2,
    )
  }

  "dense_rank over partition by firstName order by lastName" {
    val q = sql.select {
      val p = from(Table<Person>())
      p to over().partitionBy(p.firstName).sortBy(p.lastName).rankDense()
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe listOf(
      Person(3, "Jim", "Roogs", 333) to 1,
      Person(4, "Jim", "Smith", 444) to 2,
      Person(1, "Joe", "Bloggs", 111) to 1,
      Person(2, "Joe", "Doggs", 222) to 2,
    )
  }

  "row_number over partition by firstName order by lastName" {
    val q = sql.select {
      val p = from(Table<Person>())
      p to over().partitionBy(p.firstName).sortBy(p.lastName).rowNumber()
    }
    q.build<PostgresDialect>().runOn(ctx) shouldBe listOf(
      Person(3, "Jim", "Roogs", 333) to 1,
      Person(4, "Jim", "Smith", 444) to 2,
      Person(1, "Joe", "Bloggs", 111) to 1,
      Person(2, "Joe", "Doggs", 222) to 2,
    )
  }

  "sum over partition by firstName" {
    val q = sql.select {
      val p = from(Table<Person>())
      Triple(p.firstName, p.lastName, over().partitionBy(p.firstName).sum(p.age))
    }
    q.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Triple("Joe", "Bloggs", 333),
      Triple("Joe", "Doggs", 333),
      Triple("Jim", "Roogs", 777),
      Triple("Jim", "Smith", 777)
    )
  }

  "avg over partition by firstName" {
    val q = sql.select {
      val p = from(Table<Person>())
      Triple(p.firstName, p.lastName, over().partitionBy(p.firstName).avg(p.age))
    }
    q.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Triple("Joe", "Bloggs", 166.5),
      Triple("Joe", "Doggs", 166.5),
      Triple("Jim", "Roogs", 388.5),
      Triple("Jim", "Smith", 388.5)
    )
  }

  "min over partition by firstName" {
    val q = sql.select {
      val p = from(Table<Person>())
      Triple(p.firstName, p.lastName, over().partitionBy(p.firstName).min(p.age))
    }
    q.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Triple("Joe", "Bloggs", 111),
      Triple("Joe", "Doggs", 111),
      Triple("Jim", "Roogs", 333),
      Triple("Jim", "Smith", 333)
    )
  }

  "max over partition by firstName" {
    val q = sql.select {
      val p = from(Table<Person>())
      Triple(p.firstName, p.lastName, over().partitionBy(p.firstName).max(p.age))
    }
    q.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      Triple("Joe", "Bloggs", 222),
      Triple("Joe", "Doggs", 222),
      Triple("Jim", "Roogs", 444),
      Triple("Jim", "Smith", 444)
    )
  }
})
