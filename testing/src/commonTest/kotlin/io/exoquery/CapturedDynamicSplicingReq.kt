package io.exoquery

import io.exoquery.testdata.Person

class CapturedDynamicSplicingReq : GoldenSpecDynamic(CapturedDynamicSplicingReqGoldenDynamic, Mode.ExoGoldenTest(), {
//  "dynamic catpured function with join clauses" {
//    val possibleNames = listOf("Joe", "Jack", "Jill")
//
//    @CapturedDynamic
//    fun joinedClauses(p: SqlExpression<Person>) =
//      possibleNames.map { n -> sql.expression { p.use.name == param(n) } }
//        .reduce { a, b -> sql.expression { a.use || b.use } }
//
//    val filteredPeople = sql {
//      Table<Person>().filter { p -> joinedClauses(sql.expression { p }).use }
//    }
//
//    // Splice in the runtime quotes and reconstruct the query so we can golden-test it
//    val (rekeyedAndSpliced, params) = filteredPeople.rekeyRuntimeBinds().spliceQuotations()
//    val reconstructedQuery =
//      SqlQuery<Person>(rekeyedAndSpliced as XR.Query, RuntimeSet.Empty, ParamSet(params)).determinizeDynamics()
//
//    shouldBeGolden(reconstructedQuery.xr, "XR")
//    shouldBeGolden(reconstructedQuery.params, "Params")
//    shouldBeGolden(filteredPeople.build<PostgresDialect>().determinizeDynamics(), "SQL")
//  }

  "dynamic captured expression" {
    //@CapturedDynamic
    //fun nameLowerCaseSql(name: String) =
    //  sql.expression { param(name).sql.lowercase() }

    //val query = sql {
    //  Table<Person>().filter { p -> p.name == nameLowerCaseSql(p.name).use }
    //}


    @SqlDynamic
    fun nameLowerCaseSql(name: SqlExpression<String>) =
      sql.expression { name.use.sql.lowercase() }

    val query = sql {
      Table<Person>().filter { p -> p.name == nameLowerCaseSql(sql.expression { p.name }).use }
    }

    println(query.determinizeDynamics())
  }
})
