package io.exoquery

import io.exoquery.testdata.Person
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec

/**
 * This is for CapturedFunction tests involving behavior and deep IR checks. The comprehensive tests are in CapturedFunctionReq.kt.
 */
class CapturedFunctionSpec : FreeSpec({
  "static function sql - structural tests" - {
    "proto function-sql i.e. call without sql" {
      @SqlFragment
      fun joes(people: SqlQuery<Person>) = sql { people.filter { p -> p.name == "Joe" } }
      val people = sql { Table<Person>() }
      shouldThrow<MissingCaptureError> { joes(people) }
    }
  }

})
