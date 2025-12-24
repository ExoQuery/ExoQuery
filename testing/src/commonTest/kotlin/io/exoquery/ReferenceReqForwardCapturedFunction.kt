package io.exoquery

import io.exoquery.testdata.Person


class ReferenceReqForwardCapturedFunction: GoldenSpecDynamic(ReferenceReqForwardCapturedFunctionGoldenDynamic, Mode.ExoGoldenTest(), {
  "in object" - {
    "using ahead object" {
      val q = sql.select {
        val p = from(CaptureAheadObject.people("Jack"))
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested" {
      val q = sql.select {
        val p = from(CaptureAheadObjectNested.people("Mack"))
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested 2x" {
      val q = sql.select {
        val p = from(CaptureAheadObjectNested2x.people("Abe"))
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead object with nested 3x" {
      val q = sql.select {
        val p = from(CaptureAheadObjectNested3x.people("JoeJoe"))
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
        val p = from(CaptureAheadClass().people("Sam"))
        //CaptureCapObject.personName(p) to p.age
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead class with nested" {
      val q = sql.select {
        val p = from(CaptureAheadClassNested().people("Sammy"))
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }

    "using ahead class with nested 1" {
      val q = sql.select {
        val p = from(CaptureAheadClassNested1().people("Samson"))
        p
      }
      val result = q.build<PostgresDialect>().determinizeDynamics()
      shouldBeGolden(q.determinizeDynamics().xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
  }
})


object CaptureAheadObjectNested3x {
  @SqlFragment
  fun people(filter: String) = sql {
    CaptureAheadObjectNested3xForward.peopleNested(filter).filter { p -> p.name == param("JoeJoe") }
  }
}

object CaptureAheadObjectNested3xForward {
  @SqlFragment
  fun peopleNested(filter: String) = sql { CaptureAheadObjectNested3xForwardForward.peopleNestedNested(filter) }
}

object CaptureAheadObjectNested3xForwardForward {
  @SqlFragment
  fun peopleNestedNested(filter: String) = sql { Table<Person>().filter { p -> p.name == filter } }
}


object CaptureAheadObject {
  @SqlFragment
  fun people(filter: String) = sql { Table<Person>().filter { p -> p.name == filter } }
}

object CaptureAheadObjectNested {
  @SqlFragment
  fun peopleNested(filter: String) = sql { Table<Person>().filter { p -> p.name == filter } }
  @SqlFragment
  fun people(filter: String) = sql { peopleNested(filter) }
}

object CaptureAheadObjectNested2x {
  @SqlFragment
  fun people(filter: String) = sql {
    CaptureAheadObjectNested.people(filter).filter { p -> p.name == param("JoeJoe") }
  }
}

class CaptureAheadClass {
  val local = "Joe1"
  @SqlFragment
  fun people(filter: String) = sql { Table<Person>().filter { p -> p.name == param(local) && p.name == filter } }
}

class CaptureAheadClassNested {
  val local = "Joe2"
  @SqlFragment
  fun peopleNested(filter: String) = sql { Table<Person>().filter { p -> p.name == param(local) && p.name == filter } }
  @SqlFragment
  fun people(filter: String) = sql { peopleNested(filter) }
}

class CaptureAheadClassNested1 {
  val localA = "JoeA3"
  val localB = "JoeB3"
  @SqlFragment
  fun peopleNested(filter: String) = sql { Table<Person>().filter { p -> p.name == param(localA) || p.name == filter } }
  @SqlFragment
  fun people(filter: String) = sql { peopleNested(filter).filter { p -> p.name == param(localB) && p.name == filter } }
}
