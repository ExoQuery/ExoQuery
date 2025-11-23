package io.exoquery

class QueryFilterFlatteningReq: GoldenSpecDynamic(QueryFilterFlatteningReqGoldenDynamic, Mode.ExoGoldenTest(), {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val id: Int, val personId: Int, val city: String)

  "can reduce" - {
    "select-clause->triple + where with filter" {
      val queryA = sql.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.personId == p.id }
        where { p.name == "Joe" }
        Triple(p.name, p.age, a.city)
      }
      val queryB = sql { queryA.filter { t -> t.third == "NewYork" } }.dynamic()
      shouldBeGolden(queryB.xr, "XR")
      shouldBeGolden(queryB.build<PostgresDialect>())
    }

    "select-clause->value + where with filter" {
      val queryA = sql.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.personId == p.id }
        where { p.name == "Joe" }
        p.name
      }
      val queryB = sql { queryA.filter { t -> t == "JoeJoe" } }
      shouldBeGolden(queryB.xr, "XR")
      shouldBeGolden(queryB.build<PostgresDialect>())
    }

    "select stuff, (select-clause->value + where with filter(___ + stuff <pure>))" {
      data class Stuff(val extra: String)

      val queryA = sql.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.personId == p.id }
        where { p.name == "Joe" }
        p.name
      }
      val queryB = sql.select {
        val stuff = from(Table<Stuff>())
        val innerQuery = queryA.filter { t -> t == "JoeJoe" && stuff.extra == "ext" }
        innerQuery.value()
      }
      shouldBeGolden(queryB.xr, "XR")
      shouldBeGolden(queryB.build<PostgresDialect>())
    }
  }

  "cannot reduce" - {
    "anything that contains impurities e.g. impure inlines" {
      data class Stuff(val extra: String)

      val queryA = sql.select {
        val p = from(Table<Person>())
        val a = join(Table<Address>()) { a -> a.personId == p.id }
        where { p.name == "Joe" }
        p.name to free("rand()")<Int>()
      }
      val queryB = sql.select {
        val stuff = from(Table<Stuff>())
        val innerQuery = queryA.filter { t -> t.first == "JoeJoe" && stuff.extra == "ext" }
        innerQuery.value()
      }.dynamic()
      shouldBeGolden(queryB.xr, "XR")
      shouldBeGolden(queryB.build<PostgresDialect>())
    }
  }
})
