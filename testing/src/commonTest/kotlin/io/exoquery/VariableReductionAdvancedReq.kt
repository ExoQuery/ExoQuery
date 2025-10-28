@file:io.exoquery.annotation.ExoGoldenTest

package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person
import io.exoquery.testdata.Robot

class VariableReductionAdvancedReq : GoldenSpec(VariableReductionAdvancedReqGolden, {
  "variable deconstruction should work" - {
    "in simple case" {
      data class Output(val name: String, val city: String)

      val people = sql.select {
        val (p, a) = from(
          select {
            val p = from(Table<Person>())
            val a = join(Table<Address>()) { p.id == it.ownerId }
            p to a
          }
        )
        Output(p.name, a.city)
      }
      people.buildPretty<PostgresDialect>("in simple case").shouldBeGolden()
    }

    "with leaf-level props" {

      val people = sql.select {
        val (n, a) = from(
          select {
            val (id, name, age) = from(Table<Person>())
            id to name
          }
        )
        val address = join(Table<Address>()) { n == it.ownerId }
        (n to address.city)
      }
      people.buildPretty<PostgresDialect>("with leaf-level props").shouldBeGolden()
    }

    "when passed to further join" {
      data class Output(val name: String, val city: String, val robotName: String)

      val people = sql.select {
        val (p, a) = from(
          select {
            val p = from(Table<Person>())
            val a = join(Table<Address>()) { p.id == it.ownerId }
            p to a
          }
        )
        val r = join(Table<Robot>()) { p.id == it.ownerId }
        Output(p.name, a.city, r.name)
      }
      people.buildPretty<PostgresDialect>("when passed to further join").shouldBeGolden()
    }
  }
})
