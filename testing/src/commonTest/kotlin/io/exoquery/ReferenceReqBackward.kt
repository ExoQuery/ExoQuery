package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Person


object ExampleBehindObject {
  val people = sql { Table<Person>() }
}

object ExampleBehindObjectNested {
  val peopleNested = sql { Table<Person>() }
  val people = sql { peopleNested }
}

object ExampleBehindObjectNested2x {
  val people = sql {
    ExampleBehindObjectNested.people.filter { p -> p.name == param("JoeJoe") }
  }
}

class ExampleBehindClass {
  val local = "Joe"
  val people = sql { Table<Person>().filter { p -> p.name == param(local) } }
}

class ExampleBehindClassNested {
  val local = "Joe"
  val peopleNested = sql { Table<Person>().filter { p -> p.name == param(local) } }
  val people = sql { peopleNested }
}

class ExampleBehindClassNested1 {
  val localA = "Joe"
  val localB = "Joe"
  val peopleNested = sql { Table<Person>().filter { p -> p.name == param(localA) } }
  val people = sql { peopleNested.filter { p -> p.name == param(localB) } }
}


class ReferenceReqBackward: GoldenSpecDynamic(ReferenceReqBackwardGoldenDynamic, Mode.ExoGoldenTest(), {
  "in object" - {
    "using ahead object" {
      val q = sql.select {
        val p = from(ExampleBehindObject.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested" {
      val q = sql.select {
        val p = from(ExampleBehindObjectNested.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested 2x" {
      val q = sql.select {
        val p = from(ExampleBehindObjectNested2x.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
  }

  "in class" - {
    "using ahead class" {
      val q = sql.select {
        val p = from(ExampleBehindClass().people)
        //ExampleCapObject.personName(p) to p.age
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead class with nested" {
      val q = sql.select {
        val p = from(ExampleBehindClassNested().people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead class with nested 1" {
      val q = sql.select {
        val p = from(ExampleBehindClassNested1().people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
  }
})
