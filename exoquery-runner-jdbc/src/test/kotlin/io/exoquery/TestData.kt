package io.exoquery

import io.exoquery.testdata.Person
import io.exoquery.PostgresDialect
import io.exoquery.controller.jdbc.JdbcController
import io.exoquery.jdbc.runOn
import io.exoquery.testdata.PersonNullable
import io.exoquery.testdata.PersonWithId
import io.exoquery.testdata.PersonWithIdCtx

suspend fun JdbcController.people() = sql { Table<Person>() }.build<PostgresDialect>().runOn(this)

suspend fun JdbcController.peopleNullable() = sql { Table<PersonNullable>() }.build<PostgresDialect>().runOn(this)

suspend fun JdbcController.peopleWithIdCtx() = sql { Table<PersonWithIdCtx>() }.build<PostgresDialect>().runOn(this)

suspend fun JdbcController.peopleWithId() = sql { Table<PersonWithId>() }.build<PostgresDialect>().runOn(this)

suspend fun JdbcController.insertPerson(person: Person) =
  sql { insert<Person> { setParams(person).excluding(id) } }.build<PostgresDialect>().runOn(this)

suspend fun JdbcController.insertPeople() =
  people.forEach { sql { insert<Person> { setParams(it) } }.build<PostgresDialect>().runOn(this) }

suspend fun JdbcController.insertAllPeople() =
  allPeople.forEach { sql { insert<Person> { setParams(it) } }.build<PostgresDialect>().runOn(this) }

val joe = Person(1, "Joe", "Bloggs", 111)
val joe2 = Person(2, "Joe", "Doggs", 222)
val jim = Person(3, "Jim", "Roogs", 333)
val george = Person(4, "George", "Googs", 444)

val batchDeletePeople = listOf(joe, joe2, jim)
val batchInsertPeople = listOf(joe2, jim, george)

val people = listOf(joe, joe2, jim)
val allPeople = listOf(joe, joe2, jim, george)
