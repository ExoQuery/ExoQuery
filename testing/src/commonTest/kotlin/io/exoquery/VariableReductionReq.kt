package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person

class VariableReductionReq : GoldenSpecDynamic(VariableReductionReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "using it-variable should reduce to the letter `a` on the other side of the clause" {
    val people = sql.select {
      val p = from(Table<Person>())
      val a = join(Table<Address>()) { p.id == it.ownerId }
      p to a
    }
    shouldBeGolden(people.xr, "XR")
    shouldBeGolden(people.build<PostgresDialect>(), "SQL")
  }

  "node-level deconstruction shuold work" - {
    val q = sql { Table<Pair<Person, Address>>().map { (p, a) -> p.name to a.street } }
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "leaf-level deconstruction should work" - {
    "in map - single" {
      val names = sql { Table<Person>().map { (id, name, age) -> name } }
      shouldBeGolden(names.xr, "XR")
      shouldBeGolden(names.build<PostgresDialect>(), "SQL")
    }
    "in map - multi" {
      val names = sql { Table<Person>().map { (id, name, age) -> name to age } }
      shouldBeGolden(names.xr, "XR")
      shouldBeGolden(names.build<PostgresDialect>(), "SQL")
    }
    "in filter" {
      val names = sql { Table<Person>().filter { (id, name, age) -> name == "Joe" } }
      shouldBeGolden(names.xr, "XR")
      shouldBeGolden(names.build<PostgresDialect>(), "SQL")
    }
  }
})
