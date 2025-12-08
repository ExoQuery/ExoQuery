package io.exoquery

import com.github.vertical_blank.sqlformatter.SqlFormatter

fun main() {
  val myQuery =
    """
      SELECT
        ship.name AS first,
        ship.specs ->> 'engine_type' AS second,
        CAST(ship.specs ->> 'max_speed' AS DOUBLE PRECISION) AS third
      FROM
        Spacecraft ship
      WHERE
        CAST(ship.specs ->> 'max_speed' AS DOUBLE PRECISION) > 1200.0
    """.trimIndent()

  val query =
    SqlFormatter
      .extend { cfg -> cfg.plusOperators("->", "->>") }
      .format(myQuery)

  println("=================== My Query ==================\n${query}")
}
