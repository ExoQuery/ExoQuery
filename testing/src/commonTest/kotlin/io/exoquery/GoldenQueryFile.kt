package io.exoquery

import io.exoquery.printing.GoldenResult


interface GoldenQueryFile {
  val queries: Map<String, GoldenResult> get() = emptyMap()

  companion object {
    val Empty = object: GoldenQueryFile {}
  }
}


object Sample: GoldenQueryFile {
  override val queries = mapOf(
    "query1" to GoldenResult("SELECT * FROM table1"),
    "query2" to GoldenResult("SELECT * FROM table2"),
    "query3" to
        GoldenResult(
          """
        SELECT * FROM table3
        WHERE column1 = 'value1'  
        """.trimMargin()
        )
  )
}
