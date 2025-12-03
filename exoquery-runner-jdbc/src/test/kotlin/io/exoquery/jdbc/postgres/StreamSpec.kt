package io.exoquery.jdbc.postgres

import io.exoquery.testdata.Address
import io.exoquery.testdata.Person
import io.exoquery.PostgresDialect
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.testdata.Robot
import io.exoquery.jdbc.TestDatabases
import io.exoquery.sql
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.jdbc.streamOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlinx.coroutines.flow.toList

class StreamSpec : FreeSpec({
  val ctx = TestDatabases.postgres

  @OptIn(TerpalSqlUnsafe::class)
  beforeSpec {
    ctx.runActionsUnsafe(
      """
      DELETE FROM Person;
      DELETE FROM Address;
      DELETE FROM Robot;
      INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'Joe', 'Bloggs', 111);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Joe', 'Doggs', 222);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (3, 'Jim', 'Roogs', 333);
      INSERT INTO Address (ownerId, street, zip) VALUES (1, '123 Main St', '12345');
      INSERT INTO Address (ownerId, street, zip) VALUES (1, '456 Elm St', '67890');
      INSERT INTO Address (ownerId, street, zip) VALUES (2, '789 Oak St', '54321');
      INSERT INTO Robot (ownerId, model, age) VALUES (2, 'R2D2', 22);
      INSERT INTO Robot (ownerId, model, age) VALUES (3, 'C3PO', 33);
      INSERT INTO Robot (ownerId, model, age) VALUES (3, 'T100', 44);
      """
    )
  }

  "query - simple stream" {
    val q = sql { Table<Person>() }
    q.build<PostgresDialect>().streamOn(ctx).toList() shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222),
      Person(3, "Jim", "Roogs", 333)
    )
  }

  "query - filter stream" {
    val q = sql { Table<Person>().filter { it.firstName == "Joe" } }
    q.build<PostgresDialect>().streamOn(ctx).toList() shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222)
    )
  }

  "query - filter with param stream" {
    val joe = "Joe"
    val q = sql { Table<Person>().filter { it.firstName == param(joe) } }
    q.build<PostgresDialect>().streamOn(ctx).toList() shouldContainExactlyInAnyOrder listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Joe", "Doggs", 222)
    )
  }
})
