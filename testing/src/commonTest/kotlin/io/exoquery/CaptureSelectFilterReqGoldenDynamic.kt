package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object CaptureSelectFilterReqGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "capture select filter should expand correctly/test query/XR" to kt(
      """select { val u = from(Table(User)); val c = leftJoin(Table(Comment)) { c.userId == u.id }; where(u.active == 1 && c.createdAt > free("now()").invoke() - { days -> free("interval ', ${'$'}days,  days'").invoke() }.apply(30)); groupBy(u); UserCommentCount(user = u, commentCount = count_GC(c.id)) }.filter { uc -> uc.commentCount > 5 }"""
    ),
    "capture select filter should expand correctly/test query" to cr(
      """
      SELECT
        uc.user_id AS id,
        uc.user_name AS name,
        uc.user_active AS active,
        uc.commentCount
      FROM
        (
          SELECT
            u.id AS user_id,
            u.name AS user_name,
            u.active AS user_active,
            count(c.id) AS commentCount
          FROM
            "User" u
            LEFT JOIN Comment c ON c.userId = u.id
          WHERE
            u.active = 1
            AND c.createdAt > (now() - interval '30 days')
          GROUP BY
            u.id,
            u.name,
            u.active
        ) AS uc
      WHERE
        uc.commentCount > 5
      """
    ),
    "capture select filter should expand correctly/capture select filter simple/XR" to kt(
      "select { val p = from(Table(Person)); where(p.age > 18); p }.filter { ccc -> ccc.name == Main St }"
    ),
    "capture select filter should expand correctly/capture select filter simple" to cr(
      """
      SELECT
        ccc.id,
        ccc.name,
        ccc.age
      FROM
        (
          SELECT
            p.id,
            p.name,
            p.age
          FROM
            Person p
          WHERE
            p.age > 18
        ) AS ccc
      WHERE
        ccc.name = 'Main St'
      """
    ),
    "capture select filter should expand correctly/capture select(where,groupBy) filter/XR" to kt(
      "select { val p = from(Table(Person)); val a = leftJoin(Table(Address)) { a.ownerId == p.id }; where(p.age > 18); groupBy(p); p }.filter { ccc -> ccc.name == Main St }"
    ),
    "capture select filter should expand correctly/capture select(where,groupBy) filter" to cr(
      """
      SELECT
        ccc.id,
        ccc.name,
        ccc.age
      FROM
        (
          SELECT
            p.id,
            p.name,
            p.age
          FROM
            Person p
            LEFT JOIN Address a ON a.ownerId = p.id
          WHERE
            p.age > 18
          GROUP BY
            p.id,
            p.name,
            p.age
        ) AS ccc
      WHERE
        ccc.name = 'Main St'
      """
    ),
    "capture select filter should expand correctly/capture select(where,groupBy) map/XR" to kt(
      "select { val p = from(Table(Person)); val a = leftJoin(Table(Address)) { a.ownerId == p.id }; where(p.age > 18); groupBy(p); p }.map { ccc -> Tuple(first = ccc.name, second = ccc.age) }"
    ),
    "capture select filter should expand correctly/capture select(where,groupBy) map" to cr(
      """
      SELECT
        p.name AS first,
        p.age AS second
      FROM
        Person p
        LEFT JOIN Address a ON a.ownerId = p.id
      WHERE
        p.age > 18
      GROUP BY
        p.id,
        p.name,
        p.age
      """
    ),
  )
}
