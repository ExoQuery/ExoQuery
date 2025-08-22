package io.exoquery

import io.exoquery.annotation.CapturedFunction
import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Address
import io.exoquery.testdata.Person


object LimitedContainer {
  val people = capture { Table<Person>() }
  fun allPeople() = capture { people }

  @CapturedFunction
  fun peopleWithName(name: String) =
    capture { people.filter { p -> p.name == name } }
  fun allJoes() =
    capture { peopleWithName("Joe") }
}

class CapturedFunctionReq: GoldenSpecDynamic(CapturedFunctionReqGoldenDynamic, Mode.ExoGoldenTest(), {
  @CapturedFunction
  fun joes(people: SqlQuery<Person>) = capture { people.filter { p -> p.name == "Joe" } }
  val foo: Boolean = true

  "static function capture - structural tests" - {
    "cap { capFun(Table) }" {
      val capJoes = capture { joes(Table<Person>()) }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "val tbl; cap { capFun(tbl) }" {
      val tbl = capture { Table<Person>() }
      val capJoes = capture { joes(tbl) }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "val tbl; cap { capFun(tbl).filter }" {
      val tbl = capture { Table<Person>() }
      val capJoes = capture { joes(tbl).filter { p -> p.age > 21 } }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "val tbl; select { from(capFun(tbl)) }" {
      val tbl = capture { Table<Person>() }
      val capJoes = capture.select { val p = from(joes(tbl)); p }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFunA { capFunB } }" {
      @CapturedFunction
      fun jacks(people: SqlQuery<Person>) = capture { joes(people.filter { p -> p.name == "Jack" }) }
      val capJoes = capture { jacks(Table<Person>()) }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFunA(x) -> capFunB }" {
      @CapturedFunction
      fun namedX(people: SqlQuery<Person>, name: String) = capture { joes(people.filter { p -> p.name == name }) }
      val capJoes = capture { namedX(Table<Person>(), "Jack") }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFunA(x) -> capFunB(x) -> capFunC }" {
      @CapturedFunction
      fun namedX(people: SqlQuery<Person>, name: String) = capture { joes(people.filter { p -> p.name == name }) }

      @CapturedFunction
      fun namedY(people: SqlQuery<Person>, name: String) = capture { namedX(people, name) }
      val capJoes = capture { namedY(Table<Person>(), "Jack") }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
    "cap { capFunA(capFunB(x)) -> capFunC }" {
      @CapturedFunction
      fun namedX(people: SqlQuery<Person>, name: String) = capture { people.filter { p -> p.name == name } }

      @CapturedFunction
      fun namedY(people: SqlQuery<Person>, name: String) = capture { people.filter { p -> p.name == name } }
      val capJoes = capture { namedY(namedX(Table<Person>(), "Joe"), "Jack") }
      shouldBeGolden(capJoes.xr, "XR")
      shouldBeGolden(capJoes.build<PostgresDialect>(), "SQL")
    }
  }

  "advanced cases" - {
    val joes = capture { Table<Person>().filter { p -> p.name == param("joe") } }
    "passing in a param" {
      @CapturedFunction
      fun <T> joinPeopleToAddress(people: SqlQuery<T>, otherValue: String, f: (T) -> Int) =
        capture.select {
          val p = from(people)
          val a =
            join(Table<Address>()) { a -> a.ownerId == f(p) && a.street == otherValue } // should have a verification that param(otherValue) fails
          p to a
        }

      val r = "foobar"
      val result = capture {
        joinPeopleToAddress(joes, param(r)) { it.id }.map { kv -> kv.first to kv.second }
      }
      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }
    "subtype polymorphicsm" {
      val joes = capture { Table<SubtypePoly.Person>().filter { p -> p.name == param("joe") } }

      @CapturedFunction
      fun <T: SubtypePoly.HasId> joinPeopleToAddress(people: SqlQuery<T>): SqlQuery<Pair<T, Address>> =
        capture.select {
          val p = from(people)
          val a = join(Table<Address>()) { a -> a.ownerId == p.id }
          p to a
        }

      val result = capture {
        joinPeopleToAddress(joes).map { kv -> kv.first.name to kv.second.city }
      }
      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }

    "lambda polymorphism - A" {
      @CapturedFunction
      fun <T> joinPeopleToAddress(people: SqlQuery<T>, f: (T) -> Int) =
        capture.select {
          val p = from(people)
          val a = join(Table<Address>()) { a -> a.ownerId == f(p) }
          p to a
        }

      val result = capture {
        joinPeopleToAddress(joes) { it.id }.map { kv -> kv.first.name to kv.second.city }
      }
      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }

    "lambda polymorphism - B" {
      @CapturedFunction
      fun <T> joinPeopleToAddress(people: SqlQuery<T>, f: (T, Address) -> Boolean) =
        capture.select {
          val p = from(people)
          val a = join(Table<Address>()) { a -> f(p, a) }
          p to a
        }

      val joinFunction = capture.expression {
        { p: Person, a: Address -> p.id == a.ownerId }
      }

      val result =
        capture {
          joinPeopleToAddress(joes) { p, a -> joinFunction.use(p, a) }.map { kv -> kv.first.name to kv.second.city }
        }

      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }

    "lambda polymorphism - C + captured expression" {
      @CapturedFunction
      fun <T> joinPeopleToAddress(people: SqlQuery<T>, f: (T, Address) -> Boolean) =
        capture.select {
          val p = from(people)
          val a = join(Table<Address>()) { a -> f(p, a) }
          p to a
        }

      @CapturedFunction
      fun joinFunction(p: Person, a: Address) =
        capture.expression { p.id == a.ownerId }

      val result = capture {
        joinPeopleToAddress(joes) { p, a -> joinFunction(p, a).use }.map { kv -> kv.first.name to kv.second.city }
      }
      shouldBeGolden(result.determinizeDynamics().xr, "XR")
      shouldBeGolden(result.build<PostgresDialect>(), "SQL")
    }
  }

  "dynamic function capture - structural tests" - {
    "val tbl(Dyn); cap { capFun(tbl) }" {
      val tbl = if (foo) capture { Table<Person>() } else capture { Table<Person>() }
      val capJoes = capture { joes(tbl) }
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
      @CapturedFunction
      fun calculateScoresForPerson(personId: Int) =
        capture {
          Table<Person>()
            .filter { p -> p.id == personId }
            .map { it.age }
        }

      // The clause should have p1.id == p.id and not p1.id == p1.id
      val q = capture.select {
        Table<Person>().map { p -> p.id to calculateScoresForPerson(p.id).sum() }
      }
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
  }

  "limited container" - {
    "using limited container" {
      val q = capture.select {
        val p = from(LimitedContainer.allPeople())
        p
      }
      val result = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
    "using limited container with captured function" {
      val q = capture.select {
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
