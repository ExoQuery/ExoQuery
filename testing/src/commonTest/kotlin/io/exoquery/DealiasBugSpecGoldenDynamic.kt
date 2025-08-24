package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt

object DealiasBugSpecGoldenDynamic: GoldenQueryFile {
  override val queries = mapOf<String, GoldenResult>(
    "column names in a subquery with same sub-name (original bug case)/XR" to kt(
      "select { val t = from(select { val tt = from(Table(MyTable)); where(tt.id == 123); MySubTable(user = tt, id = tt.id) }); where(t.user.id == 123); Tuple(first = t.user, second = t.id) }"
    ),
    "column names in a subquery with same sub-name (original bug case)" to cr(
      """SELECT tt.id, tt."user", tt.id AS second FROM MyTable tt WHERE tt.id = 123 AND tt.id = 123"""
    ),
    "column names in a subquery with differnet sub-names/XR" to kt(
      "select { val t = from(select { val tt = from(Table(MyTable)); where(tt.id == 123); MySubTable(otherOther = tt, id = tt.id) }); where(t.otherOther.id == 123); Tuple(first = t.otherOther, second = t.id) }"
    ),
    "column names in a subquery with differnet sub-names" to cr(
      """SELECT tt.id, tt."user", tt.id AS second FROM MyTable tt WHERE tt.id = 123 AND tt.id = 123"""
    ),
    "column names in a subquery with same names but no where-clause/XR" to kt(
      "select { val t = from(select { val tt = from(Table(MyTable)); MySubTable(otherOther = tt, id = tt.id) }); where(t.otherOther.id == 123); Tuple(first = t.otherOther, second = t.id) }"
    ),
    "column names in a subquery with same names but no where-clause" to cr(
      """SELECT tt.id, tt."user", tt.id AS second FROM MyTable tt WHERE tt.id = 123"""
    ),
  )
}
