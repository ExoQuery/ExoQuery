package io.exoquery

import io.exoquery.PostgresDialect

/**
 * The purpose of this test was to originally test a bug that occurred when we had a
 * where-clause combined with a sub-table in a variable called "user" where the
 * child table also had a "user" column. An extra clause was added into SqlIdiom
 * as a band-aid in order to make sure the synthesis worked even in the case where
 * the `exo_synth_sxw` variable (that should never be used but was) is revealed and
 * and when the XR.Property `exo_synth_sxw.user` was tokenized it created the invalid
 * construct `exo_synth_sxw.` (i.e. nothing after the "."). This issue was resolved
 * by adding a missing `!alias.isUnused()` (in the 2nd dealias clause of the Dealias phase).
 * However, I decided to leave in the band-aid code in case the problem manifests again.
 */
class DealiasBugSpec: GoldenSpecDynamic(DealiasBugSpecGoldenDynamic, Mode.ExoGoldenTest(), {
  "column names in a subquery with same sub-name (original bug case)" {
    data class MyTable(val id: Int, val user: String)
    data class MySubTable(val user: MyTable, val id: Int)
    val q = sql.select {
      val t = from(sql.select {
        val tt = from(Table<MyTable>())
        where { tt.id == 123 }
        MySubTable(tt, tt.id)
      })
      where { t.user.id == 123 }
      t.user to t.id
    }.dynamic()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }

  "column names in a subquery with differnet sub-names" {
    data class MyTable(val id: Int, val user: String)
    data class MySubTable(val otherOther: MyTable, val id: Int)
    val q = sql.select {
      val t = from(sql.select {
        val tt = from(Table<MyTable>())
        where { tt.id == 123 }
        MySubTable(tt, tt.id)
      })
      where { t.otherOther.id == 123 }
      t.otherOther to t.id
    }.dynamic()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }

  "column names in a subquery with same names but no where-clause" {
    data class MyTable(val id: Int, val user: String)
    data class MySubTable(val otherOther: MyTable, val id: Int)
    val q = sql.select {
      val t = from(sql.select {
        val tt = from(Table<MyTable>())
        MySubTable(tt, tt.id)
      })
      where { t.otherOther.id == 123 }
      t.otherOther to t.id
    }.dynamic()
    shouldBeGolden(q.xr, "XR")
    shouldBeGolden(q.build<PostgresDialect>())
  }
})
