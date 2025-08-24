package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object NameEscapeSpecGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "keyword-escaping should work property for/table names/XR" to kt(
      "select { val t = from(Table(user)); where(t.id == 123); t }"
    ),
    "keyword-escaping should work property for/table names" to cr(
      """SELECT t.id, t.name FROM "user" t WHERE t.id = 123"""
    ),
    "keyword-escaping should work property for/table variable names/XR" to kt(
      "select { val user = from(Table(MyTable)); where(user.id == 123); user }"
    ),
    "keyword-escaping should work property for/table variable names" to cr(
      """SELECT "user".id, "user".name FROM MyTable "user" WHERE "user".id = 123"""
    ),
    "keyword-escaping should work property for/column names/XR" to kt(
      "select { val t = from(Table(MyTable)); where(t.id == 123); t.user }"
    ),
    "keyword-escaping should work property for/column names" to cr(
      """SELECT t."user" AS value FROM MyTable t WHERE t.id = 123"""
    ),
    "keyword-escaping should work property for/column names in a subquery/XR" to kt(
      "select { val t = from(select { val tt = from(Table(MyTable)); where(tt.id == 123); MySubTable(user = tt, id = tt.id) }.nested); where(t.user.id == 123); Tuple(first = t.user, second = t.id) }"
    ),
    "keyword-escaping should work property for/column names in a subquery" to cr(
      """SELECT t.user_id AS id, t.user_user AS "user", t.id AS second FROM (SELECT tt.id AS user_id, tt."user" AS user_user, tt.id FROM MyTable tt WHERE tt.id = 123) AS t WHERE t.user_id = 123"""
    ),
  )
}
