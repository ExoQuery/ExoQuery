package io.exoquery.jdbc.postgres

import io.exoquery.testdata.Person
import io.exoquery.PostgresDialect
import io.exoquery.jdbc.TestDatabases
import io.exoquery.sql
import io.exoquery.controller.runActions
import io.exoquery.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.to


class AggSpec : FreeSpec({
  val ctx = TestDatabases.postgres

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
