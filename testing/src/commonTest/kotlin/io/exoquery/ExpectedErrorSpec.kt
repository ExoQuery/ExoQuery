package io.exoquery

import io.exoquery.annotation.CapturedDynamic
import io.exoquery.annotation.CapturedFunction
import io.exoquery.testdata.Person
import io.kotest.core.spec.style.FreeSpec

//object CapturedFunctionParamError {
//  @CapturedFunction
//  fun capFunc(value: Int) =
//    capture.expression { param(value) } // Error: Captured function cannot have non-expression parameters
//
//  val q = capture {
//    Table<Person>().filter { p -> capFunc(p.id).use == 1 }
//  }
//}

//object CapturedDynamicParamError {
//  @CapturedDynamic
//  fun capFunc(value: Int) =
//    capture.expression { param(value) } // Error: Captured function cannot have non-expression parameters
//
//  val q = capture {
//    Table<Person>().filter { p -> capFunc(p.id).use == 1 }
//  }
//}

//object CapturedDynamicWrongParamTypeError {
//  @CapturedDynamic
//  fun capFunc(value: Int) =
//    capture.expression { value }
//
//  val q = capture {
//    Table<Person>().filter { p -> capFunc(p.id).use == 1 }
//  }
//}

class ExpectedErrorSpec : FreeSpec({
  //"captured function with non-expression parameter should error" {
  //  // Just referencing the object should trigger the error
  //  CapturedFunctionParamError.q.buildFor.Postgres()
  //}
  //"captured function with dynamic parameter should error" {
  //  // Just referencing the object should trigger the error
  //  CapturedDynamicParamError.q.buildFor.Postgres()
  //}
  //"captured dynamic with non-expression parameter should error" {
  //  // Just referencing the object should trigger the error
  //  CapturedDynamicWrongParamTypeError.q.buildFor.Postgres()
  //}
})
