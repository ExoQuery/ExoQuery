package io.exoquery.r2dbc

import io.exoquery.testdata.Person
import io.exoquery.PostgresDialect
import io.exoquery.controller.r2dbc.R2dbcController
import io.exoquery.runOn
import io.exoquery.sql
import io.exoquery.testdata.PersonNullable
import io.exoquery.testdata.PersonWithId
import io.exoquery.testdata.PersonWithIdCtx

suspend fun R2dbcController.people() = sql { Table<Person>() }.build<PostgresDialect>().runOn(this)

suspend fun R2dbcController.peopleNullable() = sql { Table<PersonNullable>() }.build<PostgresDialect>().runOn(this)

suspend fun R2dbcController.peopleWithIdCtx() = sql { Table<PersonWithIdCtx>() }.build<PostgresDialect>().runOn(this)

suspend fun R2dbcController.peopleWithId() = sql { Table<PersonWithId>() }.build<PostgresDialect>().runOn(this)

suspend fun R2dbcController.insertPerson(person: Person) =
  sql { insert<Person> { setParams(person).excluding(id) } }.build<PostgresDialect>().runOn(this)

suspend fun R2dbcController.insertPeople() =
  people.forEach { sql { insert<Person> { setParams(it) } }.build<PostgresDialect>().runOn(this) }

suspend fun R2dbcController.insertAllPeople() =
  allPeople.forEach { sql { insert<Person> { setParams(it) } }.build<PostgresDialect>().runOn(this) }

val joe = Person(1, "Joe", "Bloggs", 111)
val joe2 = Person(2, "Joe", "Doggs", 222)
val jim = Person(3, "Jim", "Roogs", 333)
val george = Person(4, "George", "Googs", 444)

val batchDeletePeople = listOf(joe, joe2, jim)
val batchInsertPeople = listOf(joe2, jim, george)

val people = listOf(joe, joe2, jim)
val allPeople = listOf(joe, joe2, jim, george)
