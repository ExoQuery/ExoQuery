package io.exoquery

import io.exoquery.annotation.CapturedFunction
import io.exoquery.annotation.TracesEnabled
import io.exoquery.util.TraceType

class MultiFieldReq: GoldenSpecDynamic(MultiFieldReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "multi groupBy should expand correctly" - {
    data class Person(val id: Int, val name: String, val age: Int)
    data class Address(val ownerId: Int, val street: String, val city: String)

    "by whole record" {
      val people = capture.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.ownerId == p.id }
        groupBy(p)
        p to count(a.street)
      }
      shouldBeGolden(people.xr, "XR")
      shouldBeGolden(people.buildPrettyFor.Postgres(), useTokenRendering = false)
    }

    "multiple-nested" {
      val a = capture.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.ownerId == p.id }
        Pair(Pair(p, a.street), a)
      }
      val b = capture.select {
        val av = from(a)
        groupBy(av.first)
        av.first to count(av.second.street)
      }.dyanmic() // TODO fix spelling

      shouldBeGolden(b.xr, "XR")
      shouldBeGolden(b.buildPrettyFor.Postgres(), useTokenRendering = false)
    }

    "multiple-nested - named record" {
      data class PersonWithStreet(val person: Person, val street: String)
      data class OuterProduct(val pws: PersonWithStreet, val address: Address)
      data class GroupedProduct(val pws: PersonWithStreet, val streetCount: Int)

      val a = capture.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.ownerId == p.id }
        OuterProduct(PersonWithStreet(p, a.street), a)
      }
      val b = capture.select {
        val av = from(a)
        groupBy(av.pws)
        GroupedProduct(av.pws, count(av.address.street))
      }.dyanmic() // TODO fix spelling

      shouldBeGolden(b.xr, "XR")
      shouldBeGolden(b.buildPrettyFor.Postgres(), useTokenRendering = false)
    }
  }
})
