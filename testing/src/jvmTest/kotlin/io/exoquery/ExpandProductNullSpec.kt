package io.exoquery

import io.exoquery.norm.ExpandReducibleNullChecks
import io.exoquery.xr.BetaReduction

class ExpandProductNullSpec: GoldenSpecDynamic(ExpandProductNullSpecGoldenDynamic, Mode.ExoGoldenTest(), {

  data class Row(val column: String?, val int: Int?)

  "Row(Column?)? used-as row?.column?.let { it + 'foo' }" {
    val expr = sql.expression { { row: Row? -> row?.column?.let { it + "foo" } } }
    val output = ExpandReducibleNullChecks(BetaReduction(expr.xr))
    shouldBeGolden(output)
  }

  "Row(Column?)? used-as row?.column?.let { it + 'foo' } ?: 'alt'" {
    val expr = sql.expression { { row: Row? -> row?.column?.let { it + "foo" } ?: "alt" } }
    val output = ExpandReducibleNullChecks(BetaReduction(expr.xr))
    shouldBeGolden(output)
  }

  "Row(IntColumn?)? used-as row?.int?.let { it == 123 } ?: 'alt'" {
    val expr = sql.expression { { row: Row? -> row?.int?.let { it == 123 } ?: "alt" } }
    val output = ExpandReducibleNullChecks(BetaReduction(expr.xr))
    shouldBeGolden(output)
  }

  "Row(IntColumn?)? used-as row?.int?.let { it == null } ?: 'alt'" {
    val expr = sql.expression { { row: Row? -> row?.int?.let { it == null } ?: "alt" } }
    val output = ExpandReducibleNullChecks(BetaReduction(expr.xr))
    shouldBeGolden(output)
  }

})
