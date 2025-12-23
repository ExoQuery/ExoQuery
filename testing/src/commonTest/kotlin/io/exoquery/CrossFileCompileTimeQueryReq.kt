package io.exoquery

import io.exoquery.PostgresDialect

class CrossFileCompileTimeQueryReq : GoldenSpecDynamic(CrossFileCompileTimeQueryReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "compile-time queries should work for" - {
    "regular functions" - {
      "inline functions defined in previous compilation units" {
        val q = sql {
          crossFileSelect().filter { pair -> pair.first.name == "JoeOuter" }
        }
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline functions defined in previous-previous compilation units - A" {
        val q = sql {
          crossFileSelectSelect().filter { pair -> pair.first.first.name == "JoeOuter" }
        }
        val result = q.build<PostgresDialect>()
        //shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline functions defined in previous-previous compilation units - B" {
        val q = sql {
          crossFileSelectExpr().filter { pair -> pair.first.name == "JoeOuter" }
        }
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline functions defined in previous compilation units used directly" {
        val q = crossFileSelect()
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline functions defined in previous-previous compilation units used directly" {
        val q = crossFileSelectSelect()
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline functions defined in previous-previous(expr) compilation units used directly" {
        val q = crossFileSelectExpr()
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
    }
    "captured functions" - {
      "inline captured functions defined in previous compilation units" {
        val q = sql {
          crossFileCapSelect(Table<PersonCrs>().filter { p -> p.name == "JoeInner" })
            .filter { pair -> pair.name == "JoeOuter" }
        }
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline captured functions defined in previous-previous compilation units" {
        val q = sql {
          crossFileCapSelectCapSelect(
            Table<PersonCrs>().filter { p -> p.name == "JoeInner" }
          ).filter { pair -> pair.first.name == "JoeOuter" }
        }
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline captured functions defined in previous-previous(expr) compilation units" {
        val q = sql {
          crossFileCapSelectCapExpr(
            Table<PersonCrs>().filter { p -> p.name == "JoeInner" }
          ).filter { pair -> pair.name == "JoeOuter" }
        }
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
    }
    // TODO not currently supported, need to think about how to allow this
    //"runtime functions" - {
    //  "inline runtime functions defined in previous compilation units" {
    //    val q = sql {
    //      crossFileDynSelect(Table<PersonCrs>().filter { p -> p.name == "JoeInner" })
    //        .filter { pair -> pair.name == "JoeOuter" }
    //    }
    //    val result = q.build<PostgresDialect>()
    //    shouldBeGolden(q.xr, "XR")
    //    shouldBeGolden(result, "SQL")
    //    shouldBeGolden(result.debugData.phase.toString(), "Phase")
    //  }
    //  "inline runtime functions defined in previous-previous compilation units" {
    //    val q = sql {
    //      crossFileDynSelectDynSelect(
    //        Table<PersonCrs>().filter { p -> p.name == "JoeInner" }
    //      ).filter { pair -> pair.first.name == "JoeOuter" }
    //    }
    //    val result = q.build<PostgresDialect>()
    //    shouldBeGolden(q.xr, "XR")
    //    shouldBeGolden(result, "SQL")
    //    shouldBeGolden(result.debugData.phase.toString(), "Phase")
    //  }
    //}
  }

  "compile-time expressions should work for" - {
    "inline functions defined in previous compilation units" {
      val q = sql {
        crossFileExpr().filter { it.name == "JoeExprOuter"  }
      }
      val result = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
    "inline functions defined in previous-previous compilation units" {
      val q = sql {
        crossFileExprExpr().filter { it.name == "JoeExprOuter"  }
      }
      val result = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
    "inline functions defined in previous compilation units used directly" {
      val q = crossFileExpr()
      val result = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
    "inline functions defined in previous-previous compilation units used directly" {
      val q = crossFileExprExpr()
      val result = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
  }

  fun complete() {
    printStoredXRs()
  }

})
