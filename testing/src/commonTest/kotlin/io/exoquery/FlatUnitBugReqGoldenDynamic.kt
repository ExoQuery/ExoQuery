package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object FlatUnitBugReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "flatUnitBug/SQL" to cr(
      "SELECT c.id AS first, c.name AS second, i.quantity AS first, i.unitPrice AS second FROM BCustomer c INNER JOIN BOrder o ON o.customerId = c.id INNER JOIN BOrderItem i ON i.orderId = o.id INNER JOIN BProduct p ON p.id = i.productId INNER JOIN BCategory cat ON cat.id = p.categoryId WHERE (cat.name = 'Electronics' OR cat.name = 'Books') AND c.status = 'active' AND c.region = 'US'"
    ),
    "flatUnitBug - groupBy/SQL" to cr(
      "SELECT c.id AS first, c.name AS second, i.quantity AS first, i.unitPrice AS second FROM (SELECT it.id, it.name, it.status, it.region FROM BCustomer it WHERE it.status = 'active' AND it.region = 'US') AS c INNER JOIN BOrder o ON o.customerId = c.id INNER JOIN BOrderItem i ON i.orderId = o.id INNER JOIN BProduct p ON p.id = i.productId INNER JOIN BCategory cat ON cat.id = p.categoryId GROUP BY c.id"
    ),
    "flatUnitBug - firstTwoFiltered/SQL" to cr(
      "SELECT c.id AS first, c.name AS second, i.quantity AS first, i.unitPrice AS second FROM BCustomer c INNER JOIN (SELECT o.id, o.customerId, o.orderNumber, o.createdAt FROM BOrder o WHERE o.orderNumber = 123) AS o ON o.customerId = c.id INNER JOIN BOrderItem i ON i.orderId = o.id INNER JOIN BProduct p ON p.id = i.productId INNER JOIN BCategory cat ON cat.id = p.categoryId WHERE (cat.name = 'Electronics' OR cat.name = 'Books') AND c.status = 'active' AND c.region = 'US'"
    ),
  )
}
