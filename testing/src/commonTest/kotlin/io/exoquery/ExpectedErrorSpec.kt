package io.exoquery

import io.kotest.core.spec.style.FreeSpec

//object CapturedFunctionParamError {
//  @SqlFunction
//  fun capFunc(value: Int) =
//    sql.expression { param(value) } // Error: Captured function cannot have non-expression parameters
//
//  val q = sql {
//    Table<Person>().filter { p -> capFunc(p.id).use == 1 }
//  }
//}

//object CapturedDynamicParamError {
//  @CapturedDynamic
//  fun capFunc(value: Int) =
//    sql.expression { param(value) } // Error: Captured function cannot have non-expression parameters
//
//  val q = sql {
//    Table<Person>().filter { p -> capFunc(p.id).use == 1 }
//  }
//}

//object CapturedDynamicWrongParamTypeError {
//  @CapturedDynamic
//  fun capFunc(value: Int) =
//    sql.expression { value }
//
//  val q = sql {
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
