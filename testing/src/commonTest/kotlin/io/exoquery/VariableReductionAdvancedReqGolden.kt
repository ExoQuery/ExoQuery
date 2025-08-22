package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr

object VariableReductionAdvancedReqGolden : GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "in simple case" to cr(
      """
      SELECT
        p.name,
        a.city
      FROM
        Person p
        INNER JOIN Address a ON p.id = a.ownerId
      """
    ),
    "with leaf-level props" to cr(
      """
      SELECT
        destruct.id AS first,
        address.city AS second
      FROM
        Person destruct
        INNER JOIN Address address ON destruct.id = address.ownerId
      """
    ),
    "when passed to further join" to cr(
      """
      SELECT
        p.name,
        a.city,
        r.name AS robotName
      FROM
        Person p
        INNER JOIN Address a ON p.id = a.ownerId
        INNER JOIN Robot r ON p.id = r.ownerId
      """
    ),
  )
}
