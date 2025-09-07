package io.exoquery

import io.exoquery.sql.PostgresDialect

class CrossFileCompileTimeQueryReq : GoldenSpecDynamic(CrossFileCompileTimeQueryReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "compile-time queries should work for" - {
    "regular functions" - {
      "inline functions defined in previous compilation units" {
        val q = capture {
          crossFileSelect().filter { pair -> pair.first.name == "JoeOuter" }
        }
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline functions defined in previous-previous compilation units" {
        val q = capture {
          crossFileSelectSelect().filter { pair -> pair.first.first.name == "JoeOuter" }
        }
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline functions defined in previous-previous compilation units" {
        val q = capture {
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
        val q = capture {
          crossFileCapSelect(Table<PersonCrs>().filter { p -> p.name == "JoeInner" })
            .filter { pair -> pair.name == "JoeOuter" }
        }
        val result = q.build<PostgresDialect>()
        shouldBeGolden(q.xr, "XR")
        shouldBeGolden(result, "SQL")
        shouldBeGolden(result.debugData.phase.toString(), "Phase")
      }
      "inline captured functions defined in previous-previous compilation units" {
        val q = capture {
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
        val q = capture {
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
    //    val q = capture {
    //      crossFileDynSelect(Table<PersonCrs>().filter { p -> p.name == "JoeInner" })
    //        .filter { pair -> pair.name == "JoeOuter" }
    //    }
    //    val result = q.build<PostgresDialect>()
    //    shouldBeGolden(q.xr, "XR")
    //    shouldBeGolden(result, "SQL")
    //    shouldBeGolden(result.debugData.phase.toString(), "Phase")
    //  }
    //  "inline runtime functions defined in previous-previous compilation units" {
    //    val q = capture {
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
      val q = capture {
        crossFileExpr().filter { it.name == "JoeExprOuter"  }
      }
      val result = q.build<PostgresDialect>()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(result, "SQL")
      shouldBeGolden(result.debugData.phase.toString(), "Phase")
    }
    "inline functions defined in previous-previous compilation units" {
      val q = capture {
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
