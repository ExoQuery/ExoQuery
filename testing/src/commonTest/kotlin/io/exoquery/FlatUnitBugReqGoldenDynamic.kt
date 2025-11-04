package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object FlatUnitBugReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "flatUnitBug/SQL" to cr(
      "SELECT it.id AS first, it.name AS second, i.quantity AS first, i.unitPrice AS second FROM BCustomer it INNER JOIN BOrder o ON o.customerId = it.id INNER JOIN BOrderItem i ON i.orderId = o.id INNER JOIN BProduct p ON p.id = i.productId INNER JOIN BCategory cat ON cat.id = p.categoryId WHERE it.status = 'active' AND it.region = 'US' AND (cat.name = 'Electronics' OR cat.name = 'Books')"
    ),
    "flatUnitBug - groupBy/SQL" to cr(
      "SELECT it.id AS first, it.name AS second, i.quantity AS first, i.unitPrice AS second FROM (SELECT it.id, it.name, it.status, it.region FROM BCustomer it WHERE it.status = 'active' AND it.region = 'US') AS it INNER JOIN BOrder o ON o.customerId = it.id INNER JOIN BOrderItem i ON i.orderId = o.id INNER JOIN BProduct p ON p.id = i.productId INNER JOIN BCategory cat ON cat.id = p.categoryId GROUP BY it.id"
    ),
    "flatUnitBug - firstTwoFiltered/SQL" to cr(
      "SELECT it.id AS first, it.name AS second, i.quantity AS first, i.unitPrice AS second FROM BCustomer it INNER JOIN (SELECT it.id, it.customerId, it.orderNumber, it.createdAt FROM BOrder it WHERE it.orderNumber = 123) AS it ON it.customerId = it.id INNER JOIN BOrderItem i ON i.orderId = it.id INNER JOIN BProduct p ON p.id = i.productId INNER JOIN BCategory cat ON cat.id = p.categoryId WHERE it.status = 'active' AND it.region = 'US' AND (cat.name = 'Electronics' OR cat.name = 'Books')"
    ),
  )
}
