package io.exoquery.r2dbc.postgres

import io.exoquery.PostgresDialect
import io.exoquery.controller.runActions
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.r2dbc.jdbc.TestDatabasesR2dbc
import io.exoquery.jdbc.runOn
import io.exoquery.sql
import io.exoquery.testdata.Person
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class AggSpec : FreeSpec({
  val ctx = R2dbcControllers.Postgres(connectionFactory = TestDatabasesR2dbc.postgres)

  beforeSpec {
    ctx.runActions(
      """
      DELETE FROM Person;
      DELETE FROM Address;
      DELETE FROM Robot;
      INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'Joe', 'Bloggs', 20);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Joe', 'Doggs', 40);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (3, 'Jim', 'Roogs', 200);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (4, 'Jim', 'Roogs', 300);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (5, 'Jim', 'Roogs', 400);
      """
    )
  }

  "simple" {
    val q = sql.select {
      val p = from(Table<Person>())
      groupBy(p.firstName)
      having { avg(p.age) < 100 }
      p.firstName to max(p.age)
    }
    q.build<PostgresDialect>().runOn(ctx) shouldContainExactlyInAnyOrder listOf(
      "Joe" to 40
    )
  }
})
