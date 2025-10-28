package io.exoquery.android

import io.exoquery.testdata.Person
import io.exoquery.SqliteDialect
import io.exoquery.sql
import io.exoquery.controller.android.AndroidDatabaseController
import io.exoquery.PostgresDialect

suspend fun AndroidDatabaseController.people() = sql { Table<Person>() }.build<PostgresDialect>().runOn(this)

suspend fun AndroidDatabaseController.insertPerson(person: Person) =
  sql { insert<Person> { setParams(person) } }.build<SqliteDialect>().runOn(this)

suspend fun AndroidDatabaseController.insertPeople() =
  people.forEach { sql { insert<Person> { setParams(it) } }.build<SqliteDialect>().runOn(this) }

suspend fun AndroidDatabaseController.insertAllPeople() =
  allPeople.forEach { sql { insert<Person> { setParams(it) } }.build<SqliteDialect>().runOn(this) }

val joe = Person(1, "Joe", "Bloggs", 111)
val joe2 = Person(2, "Joe", "Doggs", 222)
val jim = Person(3, "Jim", "Roogs", 333)
val george = Person(4, "George", "Googs", 444)
val people = listOf(joe, joe2, jim)
val allPeople = listOf(joe, joe2, jim, george)
