package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Person


class ReferenceReqForward: GoldenSpecDynamic(ReferenceReqForwardGoldenDynamic, Mode.ExoGoldenTest(), {
  "in object" - {
    "using ahead object" {
      val q = sql.select {
        val p = from(ExampleAheadObject.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested" {
      val q = sql.select {
        val p = from(ExampleAheadObjectNested.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested 2x" {
      val q = sql.select {
        val p = from(ExampleAheadObjectNested2x.people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested 3x" {
      val q = sql.select {
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
      val q = sql.select {
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
      val q = sql.select {
        val p = from(ExampleAheadClassNested().people)
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead class with nested 1" {
      val q = sql.select {
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
  val people = sql {
    ExampleAheadObjectNested3xForward.people.filter { p -> p.name == param("JoeJoe") }
  }
}

object ExampleAheadObjectNested3xForward {
  val people = sql { ExampleAheadObjectNested3xForwardForward.peopleNested }
}

object ExampleAheadObjectNested3xForwardForward {
  val peopleNested = sql { Table<Person>() }
}

object ExampleAheadObject {
  val people = sql { Table<Person>() }
}

object ExampleAheadObjectNested {
  val peopleNested = sql { Table<Person>() }
  val people = sql { peopleNested }
}

object ExampleAheadObjectNested2x {
  val people = sql {
    ExampleAheadObjectNested.people.filter { p -> p.name == param("JoeJoe") }
  }
}

class ExampleAheadClass {
  val local = "Joe"
  val people = sql { Table<Person>().filter { p -> p.name == param(local) } }
}

class ExampleAheadClassNested {
  val local = "Joe"
  val peopleNested = sql { Table<Person>().filter { p -> p.name == param(local) } }
  val people = sql { peopleNested }
}

class ExampleAheadClassNested1 {
  val localA = "Joe"
  val localB = "Joe"
  val peopleNested = sql { Table<Person>().filter { p -> p.name == param(localA) } }
  val people = sql { peopleNested.filter { p -> p.name == param(localB) } }
}
