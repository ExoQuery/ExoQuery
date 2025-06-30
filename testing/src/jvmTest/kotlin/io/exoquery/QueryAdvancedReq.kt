@file:io.exoquery.annotation.ExoGoldenTest

package io.exoquery

import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person
import io.exoquery.testdata.Robot

// Note that the 1st time you overwrite the golden file it will still fail because the compile is using teh old version
// Also note that it won't actually override the BasicQuerySanitySpecGolden file unless you change this one

// build the file BasicQuerySanitySpecGolden.kt, is that as the constructor arg
class QueryAdvancedReq: GoldenSpec(QueryAdvancedReqGolden, {

  // TODO implement and test deconstruction e.g. val (p, a) = from(Table<PersonToAddress>())
  "select clause + join + nested filters" {
    val people =
      capture.select {
        val p = from(Table<Person>().filter { p -> p.age > 18 })
        val a = join(Table<Address>().filter { a -> a.street == "123 St." }) { a -> a.ownerId == p.id }
        p to a
      }
    people.buildPretty<PostgresDialect>("select clause + join + nested filters").shouldBeGolden()
  }
  "select clauses from(nested)" {
    val people =
      capture.select {
        val pa = from(
          capture.select {
            val p = from(Table<Person>())
            val a = join(Table<Address>()) { a -> a.ownerId == p.id }
            p to a
          }
        )
        val a = join(Table<Robot>()) { r -> r.ownerId == pa.first.id }
        pa to a
      }
    people.buildPretty<PostgresDialect>("select clauses from(nested)").shouldBeGolden()
  }
  "select clauses with join(select-clause)" {
    val people =
      capture.select {
        val p = from(Table<Person>())
        val ar = join(
          capture.select {
            val a = from(Table<Address>())
            val r = join(Table<Robot>()) { r -> r.ownerId == p.id && r.ownerId == a.ownerId }
            a to r
          }
        ) { a -> a.first.ownerId == p.id }
        p to ar
      }
    people.buildPretty<PostgresDialect>("select clauses with join(select-clause)").shouldBeGolden()
  }
  "select clauses from(select-clause) + join(select-clause)" {
    // select from(person(name == "Joe").join(robot)), join(person(name == "Jim").join(address))
    val people =
      capture.select {
        val pr = from(
          capture.select {
            val p = from(Table<Person>().filter { p -> p.name == "Joe" })
            val r = join(Table<Robot>()) { r -> r.ownerId == p.id }
            p to r
          }
        )
        val pa = join(
          capture.select {
            val p = from(Table<Person>().filter { p -> p.name == "Jim" })
            val a = join(Table<Address>()) { a -> a.ownerId == p.id }
            p to a
          }
        ) { a -> a.first.id == pr.first.id }
        pr to pa
      }
    people.buildPretty<PostgresDialect>("select clauses from(select-clause) + join(select-clause)").shouldBeGolden()
  }
  "select clauses from(person.map(Robot)) + join" {
    val people =
      capture.select {
        val p = from(Table<Person>().map { p -> Robot(p.id, p.name, p.name) })
        val r = join(Table<Address>()) { a -> a.ownerId == p.ownerId }
        p to r
      }
    people.buildPretty<PostgresDialect>("select clauses from(person.map(Robot)) + join").shouldBeGolden()
  }
  "select clauses join(capture+map)" {
    val people =
      capture.select {
        val p = from(Table<Person>())
        val r = join(capture { Table<Robot>().map { r -> r.ownerId } }) { r -> r == p.id }
        p to r
      }
    people.buildPretty<PostgresDialect>("select clauses join(capture+map)").shouldBeGolden()
  }
  "capture + select-clause + filters afterward" {
    val people = capture {
      Table<Person>().flatMap { p ->
        capture.select {
          val a = from(Table<Address>().filter { a -> a.ownerId == p.id })
          val r = join(Table<Robot>()) { r -> r.ownerId == p.id }
          Triple(p, a, r)
        }
      }
    }
    people.buildPretty<PostgresDialect>("capture + select-clause + filters afterward").shouldBeGolden()
  }
  "capture + select-clause (+nested) + filters afterward" {
    val people = capture {
      Table<Person>().flatMap { p ->
        capture.select {
          val a = from(Table<Address>().filter { a -> a.ownerId == p.id }.nested())
          val r = join(Table<Robot>()) { r -> r.ownerId == p.id }
          Triple(p, a, r)
        }
      }
    }
    people.buildPretty<PostgresDialect>("capture + select-clause (+nested) + filters afterward").shouldBeGolden()
  }
  "multiple from-clauses - no filters" {
    val people =
      capture.select {
        val p = from(Table<Person>())
        val a = from(Table<Address>())
        p to a
      }
    people.buildPretty<PostgresDialect>("multiple from-clauses - no filters").shouldBeGolden()
  }
  "multiple from-clauses - filter on 2nd" {
    val people =
      capture.select {
        val p = from(Table<Person>())
        val a = from(Table<Address>().filter { a -> a.ownerId == p.id })
        p to a
      }
    people.buildPretty<PostgresDialect>("multiple from-clauses - filter on 2nd").shouldBeGolden()
  }
  "multiple from-clauses - filter on where" {
    val people =
      capture.select {
        val p = from(Table<Person>())
        val a = from(Table<Address>())
        where { p.id == a.ownerId }
        p to a
      }
    people.buildPretty<PostgresDialect>("multiple from-clauses - filter on where").shouldBeGolden()
  }


})
