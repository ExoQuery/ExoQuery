package io.exoquery

import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Person

class ActionOnConflictReq: GoldenSpecDynamic(ActionOnConflictReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "onConflictUpdate" {
    val q = capture {
      insert<Person> {
        set(name to "Joe", age to 123).onConflictUpdate(id) { excluding -> set(name to excluding.name, age to excluding.age) }
      }
    }.determinizeDynamics()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "onConflictUpdate - complex" {
    val q = capture {
      insert<Person> {
        set(name to "Joe", age to 123).onConflictUpdate(id) { excluding -> set(name to name + excluding.name, age to age + excluding.age) }
      }
    }.determinizeDynamics()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "onConflictUpdate - complex + returning" {
    val q = capture {
      insert<Person> {
        set(name to "Joe", age to 123).onConflictUpdate(id) { excluding -> set(name to name + excluding.name, age to age + excluding.age) }
      }.returning { p -> p.id }
    }.determinizeDynamics()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "onConflictUpdate - multiple" {
    val q = capture {
      insert<Person> {
        // Technically invalid SQL but we want to test the parsing
        set(name to "Joe", age to 123).onConflictUpdate(id, name) { excluding -> set(name to excluding.name, age to excluding.age) }
      }
    }.determinizeDynamics()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "onConflictUpdate - setParams" {
    val joe = Person(1, "Joe", 123)
    val q = capture {
      insert<Person> {
        setParams(joe).onConflictUpdate(id) { excluding -> set(name to excluding.name, age to excluding.age) }
      }
    }.determinizeDynamics() // determinizeDynamics needed here so the xr will have deterministic parameters
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "onConflictUpdate - setParams + exclusion" {
    val joe = Person(1, "Joe", 123)
    val q = capture {
      insert<Person> {
        setParams(joe).excluding(id).onConflictUpdate(id) { excluding -> set(name to excluding.name, age to excluding.age) }
      }
    }.determinizeDynamics()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "onConflictIgnore" {
    val q = capture {
      insert<Person> {
        set(name to "Joe", age to 123).onConflictIgnore(id)
      }
    }.determinizeDynamics()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "onConflictIgnore - multiple" {
    val q = capture {
      insert<Person> {
        set(name to "Joe", age to 123).onConflictIgnore(id, name)
      }
    }.determinizeDynamics()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "onConflictIgnore - multiple - setParams" {
    val joe = Person(1, "Joe", 123)
    val q = capture {
      insert<Person> {
        setParams(joe).onConflictIgnore(id, name)
      }
    }.determinizeDynamics()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }

  "onConflictIgnore - setParams + exclusion" {
    val joe = Person(1, "Joe", 123)
    val q = capture {
      insert<Person> {
        setParams(joe).excluding(id).onConflictIgnore(id, name)
      }
    }.determinizeDynamics()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>(), "SQL")
  }
})
