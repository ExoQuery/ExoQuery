package io.exoquery

import io.exoquery.controller.jdbc.JdbcControllers
import io.exoquery.controller.runActions
import io.exoquery.jdbc.runOn
import io.exoquery.testdata.Person
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.coroutines.runBlocking

fun main() {
  val emb = EmbeddedPostgres.start()
  val ds = emb.postgresDatabase
  val query = sql {
    Table<Person>().filter { p -> p.lastName == "Ioffe" }
  }

  runBlocking {

    val postgres = JdbcControllers.Postgres(ds)
    postgres.runActions(
      """
      CREATE TABLE Person (
        id SERIAL PRIMARY KEY,
        firstName VARCHAR(255),
        lastName VARCHAR(255),
        age INT
      );
      INSERT INTO Person (firstName, lastName, age) VALUES
        ('Leah', 'Ioffe', 9),
        ('Leib', 'Ioffe', 7),
        ('Marina', 'Ioffe', 37)
      """.trimIndent()
    )

    val output = query.buildFor.Postgres().runOn(postgres)
    println(output)
  }
}
