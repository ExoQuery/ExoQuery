package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object MultiFieldReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "multi groupBy should expand correctly/by whole record/XR" to kt(
      "select { val p = from(Table(Person)); val a = join(Table(Address)) { a.ownerId == p.id }; groupBy(p); Tuple(first = p, second = count_GC(a.street)) }"
    ),
    "multi groupBy should expand correctly/by whole record" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        count(a.street) AS second
      FROM
        Person p
        INNER JOIN Address a ON a.ownerId = p.id
      GROUP BY
        p.id,
        p.name,
        p.age
      """
    ),
    "multi groupBy should expand correctly/multiple-nested/XR" to kt(
      "select { val av = from(select { val p = from(Table(Person)); val a = join(Table(Address)) { a.ownerId == p.id }; Pair(first = Pair(first = p, second = a.street), second = a) }); groupBy(av.first); Tuple(first = av.first, second = count_GC(av.second.street)) }"
    ),
    "multi groupBy should expand correctly/multiple-nested" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        a.street AS second,
        count(a.street) AS second
      FROM
        Person p
        INNER JOIN Address a ON a.ownerId = p.id
      GROUP BY
        p.id,
        p.name,
        p.age,
        a.street
      """
    ),
    "multi groupBy should expand correctly/multiple-nested - named record/XR" to kt(
      "select { val av = from(select { val p = from(Table(Person)); val a = join(Table(Address)) { a.ownerId == p.id }; OuterProduct(pws = PersonWithStreet(person = p, street = a.street), address = a) }); groupBy(av.pws); GroupedProduct(pws = av.pws, streetCount = count_GC(av.address.street)) }"
    ),
    "multi groupBy should expand correctly/multiple-nested - named record" to cr(
      """
      SELECT
        p.id,
        p.name,
        p.age,
        a.street,
        count(a.street) AS streetCount
      FROM
        Person p
        INNER JOIN Address a ON a.ownerId = p.id
      GROUP BY
        p.id,
        p.name,
        p.age,
        a.street
      """
    ),
  )
}
