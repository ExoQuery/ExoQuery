package io.exoquery

import io.exoquery.PostgresDialect

class NameEscapeSpec: GoldenSpecDynamic(NameEscapeSpecGoldenDynamic, Mode.ExoGoldenTest(), {

  // I.e. "user" is a keyword in Postgres
  "keyword-escaping should work property for" - {
    "table names" {
      data class user(val id: Int, val name: String)
      val q = sql.select {
        val t = from(Table<user>())
        where { t.id == 123 }
        t
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "table variable names" {
      data class MyTable(val id: Int, val name: String)
      val q = sql.select {
        val user = from(Table<MyTable>())
        where { user.id == 123 }
        user
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "column names" {
      data class MyTable(val id: Int, val user: String)
      val q = sql.select {
        val t = from(Table<MyTable>())
        where { t.id == 123 }
        t.user
      }
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "column names in a subquery" {
      data class MyTable(val id: Int, val user: String)
      data class MySubTable(val user: MyTable, val id: Int)
      val q = sql.select {
        val t = from(sql.select {
          val tt = from(Table<MyTable>())
          where { tt.id == 123 }
          MySubTable(tt, tt.id)
        }.nested())
        where { t.user.id == 123 }
        t.user to t.id
      }.dyanmic()
      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }

})
