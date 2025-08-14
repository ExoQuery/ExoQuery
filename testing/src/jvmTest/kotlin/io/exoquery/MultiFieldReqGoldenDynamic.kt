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
        a.first_first_id AS id,
        a.first_first_name AS name,
        a.first_first_age AS age,
        a.first_second AS second,
        count(a.second_street) AS second
      FROM
        Person p,
        (
          SELECT
            p.id AS first_first_id,
            p.name AS first_first_name,
            p.age AS first_first_age,
            a.street AS first_second,
            a.ownerId AS second_ownerId,
            a.street AS second_street,
            a.city AS second_city
          FROM
            INNER JOIN Address a ON a.ownerId = p.id
        ) AS a
      GROUP BY
        a.first_first_id,
        a.first_first_name,
        a.first_first_age,
        a.first_second
      """
    ),
    "multi groupBy should expand correctly/multiple-nested - named record/XR" to kt(
      "select { val av = from(select { val p = from(Table(Person)); val a = join(Table(Address)) { a.ownerId == p.id }; OuterProduct(pws = PersonWithStreet(person = p, street = a.street), address = a) }); groupBy(av.pws); GroupedProduct(pws = av.pws, streetCount = count_GC(av.address.street)) }"
    ),
    "multi groupBy should expand correctly/multiple-nested - named record" to cr(
      """
      SELECT
        a.pws_person_id AS id,
        a.pws_person_name AS name,
        a.pws_person_age AS age,
        a.pws_street AS street,
        count(a.address_street) AS streetCount
      FROM
        Person p,
        (
          SELECT
            p.id AS pws_person_id,
            p.name AS pws_person_name,
            p.age AS pws_person_age,
            a.street AS pws_street,
            a.ownerId AS address_ownerId,
            a.street AS address_street,
            a.city AS address_city
          FROM
            INNER JOIN Address a ON a.ownerId = p.id
        ) AS a
      GROUP BY
        a.pws_person_id,
        a.pws_person_name,
        a.pws_person_age,
        a.pws_street
      """
    ),
  )
}
