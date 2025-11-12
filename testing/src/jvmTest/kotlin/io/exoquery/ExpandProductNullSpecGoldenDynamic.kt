package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object ExpandProductNullSpecGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "Row(Column?)? used-as row? column? let { it + 'foo' }" to kt(
      "{ row -> row.column + foo }"
    ),
    "Row(Column?)? used-as row? column? let { it + 'foo' } ?: 'alt'" to kt(
      "{ row -> if (row.column + foo == null) alt else row.column + foo }"
    ),
    "Row(IntColumn?)? used-as row? int? let { it == 123 } ?: 'alt'" to kt(
      "{ row -> if (row.int == 123 == null) alt else row.int == 123 }"
    ),
    "Row(IntColumn?)? used-as row? int? let { it == null } ?: 'alt'" to kt(
      "{ row -> if (if (row.int == null) null else row.int == null == null) alt else if (row.int == null) null else row.int == null }"
    ),
  )
}
