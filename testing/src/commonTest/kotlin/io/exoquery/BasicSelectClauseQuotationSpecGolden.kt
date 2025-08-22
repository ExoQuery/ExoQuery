package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr

object BasicSelectClauseQuotationSpecGolden: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "from + join -> (p, r)" to cr(
      "SELECT p.id, p.name, p.age, r.ownerId, r.model FROM Person p INNER JOIN Robot r ON p.id = r.ownerId"
    ),
    "from + join + leftJoin -> Custom(p, r)" to cr(
      "SELECT p.id, p.name, p.age, r.ownerId, r.model FROM Person p INNER JOIN Robot r ON p.id = r.ownerId LEFT JOIN Address a ON p.id = a.ownerId"
    ),
    "from + leftJoin -> Custom(p, r)" to cr(
      "SELECT p.id, p.name, p.age, r.ownerId, r.model FROM Person p LEFT JOIN Robot r ON p.id = r.ownerId"
    ),
    "from + join + where" to cr(
      "SELECT p.name AS value FROM Person p INNER JOIN Robot r ON p.id = r.ownerId WHERE p.name = 'Joe'"
    ),
    "from + sort(Asc,Desc)" to cr(
      "SELECT p.name AS value FROM Person p ORDER BY p.age ASC, p.name DESC"
    ),
    "from + sort(Asc)" to cr(
      "SELECT p.name AS value FROM Person p ORDER BY p.age ASC"
    ),
    "from + groupBy" to cr(
      "SELECT p.age AS value FROM Person p GROUP BY p.age"
    ),
    "from + groupBy(a, b)" to cr(
      "SELECT p.age AS value FROM Person p GROUP BY p.age, p.name"
    ),
  )
}
