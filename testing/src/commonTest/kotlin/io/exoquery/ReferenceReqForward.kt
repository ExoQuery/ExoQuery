package io.exoquery

import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Person


class ReferenceReqForward: GoldenSpecDynamic(ReferenceReqForwardGoldenDynamic, Mode.ExoGoldenTest(), {
  "in object" - {
    "using ahead object" {
      val q = capture.select {
        val p = from(ExampleAheadObject.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested" {
      val q = capture.select {
        val p = from(ExampleAheadObjectNested.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested 2x" {
      val q = capture.select {
        val p = from(ExampleAheadObjectNested2x.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested 3x" {
      val q = capture.select {
        val p = from(ExampleAheadObjectNested3x.people)
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
        val p = from(ExampleAheadClass().people)
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
        val p = from(ExampleAheadClassNested().people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead class with nested 1" {
      val q = capture.select {
        val p = from(ExampleAheadClassNested1().people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
  }
})

object ExampleAheadObjectNested3x {
  val people = capture {
    ExampleAheadObjectNested3xForward.people.filter { p -> p.name == param("JoeJoe") }
  }
}

object ExampleAheadObjectNested3xForward {
  val people = capture { ExampleAheadObjectNested3xForwardForward.peopleNested }
}

object ExampleAheadObjectNested3xForwardForward {
  val peopleNested = capture { Table<Person>() }
}

object ExampleAheadObject {
  val people = capture { Table<Person>() }
}

object ExampleAheadObjectNested {
  val peopleNested = capture { Table<Person>() }
  val people = capture { peopleNested }
}

object ExampleAheadObjectNested2x {
  val people = capture {
    ExampleAheadObjectNested.people.filter { p -> p.name == param("JoeJoe") }
  }
}

class ExampleAheadClass {
  val local = "Joe"
  val people = capture { Table<Person>().filter { p -> p.name == param(local) } }
}

class ExampleAheadClassNested {
  val local = "Joe"
  val peopleNested = capture { Table<Person>().filter { p -> p.name == param(local) } }
  val people = capture { peopleNested }
}

class ExampleAheadClassNested1 {
  val localA = "Joe"
  val localB = "Joe"
  val peopleNested = capture { Table<Person>().filter { p -> p.name == param(localA) } }
  val people = capture { peopleNested.filter { p -> p.name == param(localB) } }
}
