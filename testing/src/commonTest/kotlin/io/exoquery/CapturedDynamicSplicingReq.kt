package io.exoquery

import io.exoquery.annotation.CapturedDynamic
import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Person
import io.exoquery.xr.XR
import io.exoquery.xr.rekeyRuntimeBinds
import io.exoquery.xr.spliceQuotations

class CapturedDynamicSplicingReq : GoldenSpecDynamic(CapturedDynamicSplicingReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "dynamic catpured function with join clauses" {
    val possibleNames = listOf("Joe", "Jack", "Jill")

    @CapturedDynamic
    fun joinedClauses(p: SqlExpression<Person>) =
      possibleNames.map { n -> capture.expression { p.use.name == param(n) } }
        .reduce { a, b -> capture.expression { a.use || b.use } }

    val filteredPeople = capture {
      Table<Person>().filter { p -> joinedClauses(capture.expression { p }).use }
    }

    // Splice in the runtime quotes and reconstruct the query so we can golden-test it
    val (rekeyedAndSpliced, params) = filteredPeople.rekeyRuntimeBinds().spliceQuotations()
    val reconstructedQuery =
      SqlQuery<Person>(rekeyedAndSpliced as XR.Query, RuntimeSet.Empty, ParamSet(params)).determinizeDynamics()

    shouldBeGolden(reconstructedQuery.xr, "XR")
    shouldBeGolden(reconstructedQuery.params, "Params")
    shouldBeGolden(filteredPeople.build<PostgresDialect>().determinizeDynamics(), "SQL")
  }
})
