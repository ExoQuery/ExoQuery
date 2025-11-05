package io.exoquery

import io.exoquery.annotation.SqlFragment
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person


object LimitedContainer {
  val people = sql { Table<Person>() }
  fun allPeople() = sql { people }

  @SqlFragment
  fun peopleWithName(name: String) =
    sql { people.filter { p -> p.name == name } }
  fun allJoes() =
    sql { peopleWithName("Joe") }
}

class CapturedFunctionReq: GoldenSpecDynamic(CapturedFunctionReqGoldenDynamic, Mode.ExoGoldenTest(), {
  @SqlFragment
  fun joes(people: SqlQuery<Person>) = sql { people.filter { p -> p.name == "Joe" } }
  val foo: Boolean = true

  "static function capture - structural tests" - {
    "cap { capFun(Table) }" {
      val capJoes = sql { joes(Table<Person>()) }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "val tbl; cap { capFun(tbl) }" {
      val tbl = sql { Table<Person>() }
      val capJoes = sql { joes(tbl) }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "val tbl; cap { capFun(tbl).filter }" {
      val tbl = sql { Table<Person>() }
      val capJoes = sql { joes(tbl).filter { p -> p.age > 21 } }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "val tbl; select { from(capFun(tbl)) }" {
      val tbl = sql { Table<Person>() }
      val capJoes = sql.select { val p = from(joes(tbl)); p }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFunA { capFunB } }" {
      @SqlFragment
      fun jacks(people: SqlQuery<Person>) = sql { joes(people.filter { p -> p.name == "Jack" }) }
      val capJoes = sql { jacks(Table<Person>()) }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFunA(x) -> capFunB }" {
      @SqlFragment
      fun namedX(people: SqlQuery<Person>, name: String) = sql { joes(people.filter { p -> p.name == name }) }
      val capJoes = sql { namedX(Table<Person>(), "Jack") }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFunA(x) -> capFunB(x) -> capFunC }" {
      @SqlFragment
      fun namedX(people: SqlQuery<Person>, name: String) = sql { joes(people.filter { p -> p.name == name }) }

      @SqlFragment
      fun namedY(people: SqlQuery<Person>, name: String) = sql { namedX(people, name) }
      val capJoes = sql { namedY(Table<Person>(), "Jack") }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFunA(capFunB(x)) -> capFunC }" {
      @SqlFragment
      fun namedX(people: SqlQuery<Person>, name: String) = sql { people.filter { p -> p.name == name } }

      @SqlFragment
      fun namedY(people: SqlQuery<Person>, name: String) = sql { people.filter { p -> p.name == name } }
      val capJoes = sql { namedY(namedX(Table<Person>(), "Joe"), "Jack") }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
  }

  "zero args support" - {
    "cap { capFun() }" {
      @SqlFragment
      fun allPeople() = sql { Table<Person>() }
      val capAll = sql { allPeople() }
      shouldBeGolden(capAll.xr, "XR")
      shouldBeGolden(capAll.build<PostgresDialect>(), "SQL")
    }
    "cap { capFun() -> capFunB() }" {
      @SqlFragment
      fun allPeople() = sql { Table<Person>() }

      @SqlFragment
      fun allJoes() = sql { allPeople().filter { p -> p.name == "Joe" } }
      val capJoes = sql { allJoes() }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFun() -> capFunB(X) }" {
      @SqlFragment
      fun peopleWithName(name: String) = sql { Table<Person>().filter { p -> p.name == name } }

      @SqlFragment
      fun allJoes() = sql { peopleWithName("Joe") }
      val capJoes = sql { allJoes() }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFun(X) -> capFunB() }" {
      @SqlFragment
      fun adults() = sql { Table<Person>().filter { p -> p.age > 21 } }

      @SqlFragment
      fun namedPeople(name: String) = sql { adults().filter { p -> p.name == name } }
      val capJoes = sql { namedPeople("Joe") }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
  }

  "advanced cases" - {
    val joes = sql { Table<Person>().filter { p -> p.name == param("joe") } }
    "passing in a param" {
      @SqlFragment
      fun <T> joinPeopleToAddress(people: SqlQuery<T>, otherValue: String, f: (T) -> Int) =
        sql.select {
          val p = from(people)
          val a =
            join(Table<Address>()) { a -> a.ownerId == f(p) && a.street == otherValue } // should have a verification that param(otherValue) fails
          p to a
        }

      val r = "foobar"
      val result = sql {
        joinPeopleToAddress(joes, param(r)) { it.id }.map { kv -> kv.first to kv.second }
      }
      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }
    "subtype polymorphicsm" {
      val joes = sql { Table<SubtypePoly.Person>().filter { p -> p.name == param("joe") } }

      @SqlFragment
      fun <T: SubtypePoly.HasId> joinPeopleToAddress(people: SqlQuery<T>): SqlQuery<Pair<T, Address>> =
        sql.select {
          val p = from(people)
          val a = join(Table<Address>()) { a -> a.ownerId == p.id }
          p to a
        }

      val result = sql {
        joinPeopleToAddress(joes).map { kv -> kv.first.name to kv.second.city }
      }
      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }

    "lambda polymorphism - A" {
      @SqlFragment
      fun <T> joinPeopleToAddress(people: SqlQuery<T>, f: (T) -> Int) =
        sql.select {
          val p = from(people)
          val a = join(Table<Address>()) { a -> a.ownerId == f(p) }
          p to a
        }

      val result = sql {
        joinPeopleToAddress(joes) { it.id }.map { kv -> kv.first.name to kv.second.city }
      }
      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }

    "lambda polymorphism - B" {
      @SqlFragment
      fun <T> joinPeopleToAddress(people: SqlQuery<T>, f: (T, Address) -> Boolean) =
        sql.select {
          val p = from(people)
          val a = join(Table<Address>()) { a -> f(p, a) }
          p to a
        }

      val joinFunction = sql.expression {
        { p: Person, a: Address -> p.id == a.ownerId }
      }

      val result =
        sql {
          joinPeopleToAddress(joes) { p, a -> joinFunction.use(p, a) }.map { kv -> kv.first.name to kv.second.city }
        }

      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }

    "lambda polymorphism - C + captured expression" {
      @SqlFragment
      fun <T> joinPeopleToAddress(people: SqlQuery<T>, f: (T, Address) -> Boolean) =
        sql.select {
          val p = from(people)
          val a = join(Table<Address>()) { a -> f(p, a) }
          p to a
        }

      @SqlFragment
      fun joinFunction(p: Person, a: Address) =
        sql.expression { p.id == a.ownerId }

      val result = sql {
        joinPeopleToAddress(joes) { p, a -> joinFunction(p, a).use }.map { kv -> kv.first.name to kv.second.city }
      }
      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }
  }

  "dynamic function capture - structural tests" - {
    "val tbl(Dyn); cap { capFun(tbl) }" {
      val tbl = if (foo) sql { Table<Person>() } else sql { Table<Person>() }
      val capJoes = sql { joes(tbl) }
      val det = capJoes.determinizeDynamics()
      shouldBeGolden(tbl.determinizeDynamics().xr, "XR-tbl")
      shouldBeGolden(det.determinizeDynamics().xr, "XR")
      shouldBeGolden(det.runtimes.runtimes.first().second.xr, "XR->Runimtes.first.XR")
      shouldBeTrueInGolden { det.runtimes.runtimes.first().second.xr == tbl.xr }
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
  }

  "shadowing" - {
    "query with variable shadow" {
      @SqlFragment
      fun calculateScoresForPerson(personId: Int) =
        sql {
          Table<Person>()
            .filter { p -> p.id == personId }
            .map { it.age }
        }

      // The clause should have p1.id == p.id and not p1.id == p1.id
      val q = sql.select {
        Table<Person>().map { p -> p.id to calculateScoresForPerson(p.id).sum() }
      }
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
  }

  "limited container" - {
    "using limited container" {
      val q = sql.select {
        val p = from(LimitedContainer.allPeople())
        p
      }
      val result = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
    "using limited container with captured function" {
      val q = sql.select {
        val p = from(LimitedContainer.allJoes())
        p
      }
      val result = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
  }
})

object SubtypePoly {
  interface HasId {
    val id: Int
  }

  data class Person(override val id: Int, val name: String, val age: Int): HasId
}
