package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Person

class BatchActionReq: GoldenSpecDynamic(BatchActionReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "insert" - {
    val people = listOf(Person(1, "John", 42), Person(2, "Jane", 43), Person(3, "Jack", 44)).asSequence()

    "simple" {
      val q = sql.batch(people) { p ->
        insert<Person> { set(name to param(p.name), age to param(p.age)) }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      // Note each group name needs to be unique or they'll conflict and you'll get something like:
      // Golden query for label: "insert/simple/Params" did not match
      // Expected :INSERT INTO Person (name, age) VALUES ({0:Jack}, {1:44})
      // Actual   :INSERT INTO Person (name, age) VALUES ({0:John}, {1:42})
      // TODO probably need to have some mechanism to detect this and throw a more helpful error (e.g. "group names must be unique")
      shouldBeGolden(groups[0], "Params1")
      shouldBeGolden(groups[1], "Params2")
      shouldBeGolden(groups[2], "Params3")
      shouldBeGoldenParams(groups)
    }

    "simple with setParams" {
      val q = sql.batch(people) { p ->
        insert<Person> { setParams(p) }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics().withNonStrictEquality()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
    }

    "simple with setParams and exclusion" {
      val q = sql.batch(people) { p ->
        insert<Person> { setParams(p).excluding(id) }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
    }

    "with returning" {
      val q = sql.batch(people) { p ->
        insert<Person> { set(name to param(p.name), age to param(p.age)) }.returning { p -> p.id }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
      shouldBeGolden(b.actionReturningKind.toString(), "returningType")
    }

    "with returning and params" {
      val q = sql.batch(people) { p ->
        insert<Person> { set(name to param(p.name), age to param(p.age)) }.returning { pp -> pp.id to param(p.name) }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
      shouldBeGolden(b.actionReturningKind.toString(), "returningType")
    }

    "with returning keys" {
      val q = sql.batch(people) { p ->
        insert<Person> { set(name to param(p.name), age to param(p.age)) }.returningKeys { id }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
      shouldBeGolden(b.actionReturningKind.toString(), "returningType")
    }
  }
  "update" - {
    val updatedPeople = listOf(Person(1, "John-A", 52), Person(2, "Jane-A", 53), Person(3, "Jack-A", 54)).asSequence()

    "simple" {
      val q = sql.batch(updatedPeople) { p ->
        update<Person> { set(name to param(p.name), age to param(p.age)) }.filter { pp -> pp.id == param(p.id) }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGolden(groups[0], "Params1")
      shouldBeGolden(groups[1], "Params2")
      shouldBeGolden(groups[2], "Params3")
      shouldBeGoldenParams(groups)
      shouldBeGolden(b.actionReturningKind.toString(), "returningType")
    }

    "simple - where clause" {
      val q = sql.batch(updatedPeople) { p ->
        update<Person> { set(name to param(p.name), age to param(p.age)) }.where { id == param(p.id) && age > 50 }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
    }

    "simple with setParams and exclusion" {
      val q = sql.batch(updatedPeople) { p ->
        update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.id == param(p.id) }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
    }

    "simple with setParams and exclusion and returning param" {
      val q = sql.batch(updatedPeople) { p ->
        update<Person> { setParams(p).excluding(id) }.filter { pp -> pp.id == param(p.id) }
          .returning { pp -> pp.id to param(p.name) }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
      shouldBeGolden(b.actionReturningKind.toString(), "returningType")
    }
  }

  "delete" - {
    "with filter - inside" {
      val q = sql.batch(listOf(1, 2, 3).asSequence()) { id ->
        delete<Person>().filter { it.id == param(id) }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGolden(groups[0], "Params1")
      shouldBeGolden(groups[1], "Params2")
      shouldBeGolden(groups[2], "Params3")
      shouldBeGoldenParams(groups)
    }
    "with filter" {
      val ids = listOf(1, 2, 3).asSequence()
      val q = sql.batch(ids) { id ->
        delete<Person>().filter { p -> p.id == param(id) }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
    }
    "with returning" {
      val ids = listOf(1, 2, 3).asSequence()
      val q = sql.batch(ids) { id ->
        delete<Person>().filter { p -> p.id == param(id) }.returning { p -> p.id }
      }
      val b = q.build<PostgresDialect>().determinizeDynamics()
      val groups = b.produceBatchGroups().toList()
      shouldBeGolden(b, "SQL")
      shouldBeGoldenParams(groups)
      shouldBeGolden(b.actionReturningKind.toString(), "returningType")
    }
  }
})
