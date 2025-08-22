package io.exoquery

import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Person


object ExampleBehindObject {
  val people = capture { Table<Person>() }
}

object ExampleBehindObjectNested {
  val peopleNested = capture { Table<Person>() }
  val people = capture { peopleNested }
}

object ExampleBehindObjectNested2x {
  val people = capture {
    ExampleBehindObjectNested.people.filter { p -> p.name == param("JoeJoe") }
  }
}

class ExampleBehindClass {
  val local = "Joe"
  val people = capture { Table<Person>().filter { p -> p.name == param(local) } }
}

class ExampleBehindClassNested {
  val local = "Joe"
  val peopleNested = capture { Table<Person>().filter { p -> p.name == param(local) } }
  val people = capture { peopleNested }
}

class ExampleBehindClassNested1 {
  val localA = "Joe"
  val localB = "Joe"
  val peopleNested = capture { Table<Person>().filter { p -> p.name == param(localA) } }
  val people = capture { peopleNested.filter { p -> p.name == param(localB) } }
}


class ReferenceReqBackward: GoldenSpecDynamic(ReferenceReqBackwardGoldenDynamic, Mode.ExoGoldenTest(), {
  "in object" - {
    "using ahead object" {
      val q = capture.select {
        val p = from(ExampleBehindObject.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested" {
      val q = capture.select {
        val p = from(ExampleBehindObjectNested.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested 2x" {
      val q = capture.select {
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
      val q = capture.select {
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
      val q = capture.select {
        val p = from(ExampleBehindClassNested().people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead class with nested 1" {
      val q = capture.select {
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
