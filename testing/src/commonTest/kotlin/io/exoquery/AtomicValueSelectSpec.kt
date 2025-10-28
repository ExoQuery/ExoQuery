@file:io.exoquery.annotation.ExoGoldenTest

package io.exoquery

import io.exoquery.PostgresDialect

class AtomicValueSelectSpec: GoldenSpec(AtomicValueSelectSpecGolden, {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Robot(val ownerId: Int, val model: String)
  data class Address(val ownerId: Int, val street: String, val zip: Int)

  "parsing features spec" - {
    // apply-map takes care of the non-nested case (should have that too)
    "from(atom.nested) + join -> (p, r)" {
      val names = sql { Table<Person>().map { p -> p.name }.nested() }
      val people =
        sql.select {
          val n = from(names)
          val r = join(Table<Robot>()) { r -> n == r.model }
          n to r
        }
      people.buildPretty<PostgresDialect>("from(atom.nested) + join -> (p, r)").shouldBeGolden()
    }
    "from(atom.nested.nested) + join -> (p, r)" {
      val names = sql { Table<Person>().map { p -> p.name }.nested().nested() }
      val people =
        sql.select {
          val n = from(names)
          val r = join(Table<Robot>()) { r -> n == r.model }
          n to r
        }
      people.buildPretty<PostgresDialect>("from(atom.nested.nested) + join -> (p, r)").shouldBeGolden()
    }
    "groupBy(n.nested) -> join(n)" {
      val names = sql.select {
        val n = from(Table<Person>().map { p -> p.name }.nested())
        groupBy(n)
        n
      }
      val addresses = sql.select {
        val n = from(names)
        val a = join(Table<Address>()) { a -> n == a.street }
        n to a
      }
      addresses.buildPretty<PostgresDialect>("groupBy(n.nested) -> join(n)").shouldBeGolden()
    }

    // TODO 2-table select with atomic value
    // TODO distinct on atomic-value
  }
})
