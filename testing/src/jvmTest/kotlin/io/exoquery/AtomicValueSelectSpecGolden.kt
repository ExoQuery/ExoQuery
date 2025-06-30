package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object AtomicValueSelectSpecGolden: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "from(atom.nested) + join -> (p, r)" to cr(
      """
      SELECT
        n.value AS first,
        r.ownerId,
        r.model
      FROM
        (
          SELECT
            p.name AS value
          FROM
            Person p
        ) AS n
        INNER JOIN Robot r ON n.value = r.model
      """
    ),
    "from(atom.nested.nested) + join -> (p, r)" to cr(
      """
      SELECT
        n.value AS first,
        r.ownerId,
        r.model
      FROM
        (
          SELECT
            x.value
          FROM
            (
              SELECT
                p.name AS value
              FROM
                Person p
            ) AS x
        ) AS n
        INNER JOIN Robot r ON n.value = r.model
      """
    ),
    "groupBy(n.nested) -> join(n)" to cr(
      """
      SELECT
        n.value AS first,
        a.ownerId,
        a.street,
        a.zip
      FROM
        (
          SELECT
            n.value_value AS value
          FROM
            (
              SELECT
                p.name AS value
              FROM
                Person p
            ) AS n
          GROUP BY
            n.value_value
        ) AS n
        INNER JOIN Address a ON n.value = a.street
      """
    ),
  )
}
