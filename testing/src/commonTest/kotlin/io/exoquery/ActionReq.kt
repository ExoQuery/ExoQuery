package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Person
import io.exoquery.testdata.PersonNullable

class ActionReq: GoldenSpecDynamic(ActionReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "insert" - {
    "simple" {
      val q = sql {
        insert<Person> { set(name to "Joe", age to 123) }
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "simple - nullable" {
      val q = sql {
        insert<PersonNullable> { set(name to "Joe", age to 123) }
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "simple with params" {
      val q = sql {
        insert<Person> { set(name to param("Joe"), age to param(123)) }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "simple with params - nullable" {
      val q = sql {
        insert<PersonNullable> { set(name to param("Joe"), age to param(123)) }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "simple with params - nullable - actual null" {
      val v: String? = null
      val q = sql {
        insert<PersonNullable> { set(name to param(v), age to param(123)) }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "simple with setParams" {
      val q = sql {
        insert<Person> { setParams(Person(1, "Joe", 123)) }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "simple with setParams - nullable" {
      val q = sql {
        insert<PersonNullable> { setParams(PersonNullable(1, "Joe", 123)) }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "simple with setParams - nullable - actual null" {
      val q = sql {
        insert<PersonNullable> { setParams(PersonNullable(1, null, 123)) }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "simple with setParams and exclusion" {
      val q = sql {
        insert<Person> { setParams(Person(1, "Joe", 123)).excluding(id) }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "simple with setParams and exclusion - multiple" {
      val q = sql {
        insert<Person> { setParams(Person(1, "Joe", 123)).excluding(id, name) }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "with returning" {
      val q = sql {
        insert<Person> { set(name to "Joe", age to 123) }.returning { p -> p.id }
      }
      val build = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(build, "SQL")
      shouldBeGolden(q.buildFor.SqlServer(), "SQL-SqlServer")
      shouldBeGolden(build.actionReturningKind.toString(), "returningType")
    }
    "with returning - multiple" {
      val q = sql {
        insert<Person> { set(name to "Joe", age to 123) }.returning { p -> p.id to p.name }
      }
      val build = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(build, "SQL")
      shouldBeGolden(q.buildFor.SqlServer(), "SQL-SqlServer")
      shouldBeGolden(build.actionReturningKind.toString(), "returningType")
    }
    "with returning params" {
      val myParam = "myParamValue"
      val q = sql {
        insert<Person> { set(name to param("Joe"), age to param(123)) }.returning { p -> p.name to param(myParam) }
      }.determinizeDynamics()
      val build = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(build, "SQL")
      shouldBeGoldenParams(build, "Params")
      shouldBeGolden(build.actionReturningKind.toString(), "returningType")
    }
    "with returningKeys" {
      val q = sql {
        insert<Person> { set(name to "Joe", age to 123) }.returningKeys { id }
      }
      val build = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(build, "SQL")
      shouldBeGolden(build.actionReturningKind.toString(), "returningType")
    }
    "with returningKeys - multiple" {
      val q = sql {
        insert<Person> { set(name to "Joe", age to 123) }.returningKeys { id to name }
      }
      val build = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(build, "SQL")
      shouldBeGolden(build.actionReturningKind.toString(), "returningType")
    }
  }

  "update" - {
    "simple" {
      val q = sql {
        update<Person> { set(name to "Joe", age to 123) }.filter { p -> p.id == 1 }
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "no condition" {
      val q = sql {
        update<Person> { set(name to "Joe", age to 123) }.all()
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "with setParams" {
      val q = sql {
        update<Person> { setParams(Person(1, "Joe", 123)) }.filter { p -> p.id == 1 }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "with setParams and exclusion" {
      val q = sql {
        update<Person> { setParams(Person(1, "Joe", 123)).excluding(id) }.filter { p -> p.id == 1 }
      }.determinizeDynamics()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "with returning" {
      val q = sql {
        update<Person> { set(name to "Joe", age to 123) }.filter { p -> p.id == 1 }.returning { p -> p.id }
      }
      val build = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(build, "SQL")
      shouldBeGolden(q.buildFor.SqlServer(), "SQL-SqlServer")
      shouldBeGolden(build.actionReturningKind.toString(), "returningType")
    }
    "with returningKeys" {
      val q = sql {
        update<Person> { set(name to "Joe", age to 123) }.filter { p -> p.id == 1 }.returningKeys { id }
      }
      val build = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(build, "SQL")
      shouldBeGolden(build.actionReturningKind.toString(), "returningType")
    }
  }

  "delete" - {
    "simple" {
      val q = sql {
        delete<Person>().filter { p -> p.id == 1 }
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "no condition" {
      val q = sql {
        delete<Person>().all()
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>(), "SQL")
    }
    "with returning" {
      val q = sql {
        delete<Person>().filter { p -> p.id == 1 }.returning { p -> p.id }
      }
      val build = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(build, "SQL")
      shouldBeGolden(build.actionReturningKind.toString(), "returningType")
    }
    "with returningKeys" {
      val q = sql {
        delete<Person>().filter { p -> p.id == 1 }.returningKeys { id }
      }
      val build = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(build, "SQL")
      shouldBeGolden(build.actionReturningKind.toString(), "returningType")
    }
  }
})
