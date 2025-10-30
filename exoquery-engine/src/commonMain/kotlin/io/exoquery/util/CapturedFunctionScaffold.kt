package io.exoquery.util

import io.exoquery.SqlQuery
import io.exoquery.errorCap

fun <T> scaffoldCapFunctionQuery(query: T, vararg values: Any?): T =
// The purpose of this function is to carry around the arguments of a compile-time transformed @SqlFunction instance.
  // It should not be called anywhere in the runtime.
  errorCap("A @SqlFragment call cannot be used outside of a sql block")
