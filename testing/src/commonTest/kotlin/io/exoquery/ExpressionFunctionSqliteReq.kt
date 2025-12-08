package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Person

class ExpressionFunctionSqliteReq : GoldenSpecDynamic(ExpressionFunctionSqliteReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "String" - {
    "toInt" {
      val q = sql { Table<Person>().map { p -> p.name.toInt() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "toLong" {
      val q = sql { Table<Person>().map { p -> p.name.toLong() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "toBoolean" {
      val q = sql { Table<Person>().map { p -> p.name.toBoolean() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "casting" {
      val q = sql { Table<Person>().map { p -> p.name + (p.age as String) } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "string concat" {
      val q = sql { Table<Person>().map { p -> p.name + " " + p.name } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "substr - regular" {
      val q = sql { Table<Person>().map { p -> p.name.substring(1, 2) } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "substr" {
      val q = sql { Table<Person>().map { p -> p.name.sql.substring(1, 2) } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "left" {
      val q = sql { Table<Person>().map { p -> p.name.sql.left(2) } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "right" {
      val q = sql { Table<Person>().map { p -> p.name.sql.right(2) } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "replace" {
      val q = sql { Table<Person>().map { p -> p.name.sql.replace("a", "b") } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "upper" {
      val q = sql { Table<Person>().map { p -> p.name.uppercase() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "upper - sql" {
      val q = sql { Table<Person>().map { p -> p.name.sql.uppercase() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "contains" {
      val q = sql { Table<Person>().map { p -> p.name.contains("ack") } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "lower" {
      val q = sql { Table<Person>().map { p -> p.name.lowercase() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "lower - sql" {
      val q = sql { Table<Person>().map { p -> p.name.sql.lowercase() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "trim" {
      val q = sql { Table<Person>().map { p -> p.name.trim() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "trimBoth" {
      val q = sql { Table<Person>().map { p -> p.name.sql.trimBoth("x") } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "trimRight" {
      val q = sql { Table<Person>().map { p -> p.name.sql.trimRight("x") } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "trimLeft" {
      val q = sql { Table<Person>().map { p -> p.name.sql.trimLeft("x") } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "like" {
      val q = sql { Table<Person>().map { p -> p.name.like("J%") } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "ilike" {
      val q = sql { Table<Person>().map { p -> p.name.ilike("j%") } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
  }
  "StringInterpolation" - {
    "multiple elements" {
      val q = sql { Table<Person>().map { p -> "${p.name}-${p.age}" } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "single element" {
      val q = sql { Table<Person>().map { p -> "${p.name}" } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "single constant" {
      val q = sql { Table<Person>().map { p -> "${"constant"}" } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "many elements" {
      data class LotsOfFields(
        val name: String,
        val age: Int,
        val address: String,
        val phone: String
      )
      val q = sql { Table<LotsOfFields>().map { l ->
        "${l.name}-foo-${l.age}-bar-${l.address}-baz-${l.phone}-blin"
      } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
  }
  "Int" - {
    "toLong" {
      val q = sql { Table<Person>().map { p -> p.age.toLong() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "toDouble" {
      val q = sql { Table<Person>().map { p -> p.age.toDouble() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "toFloat" {
      val q = sql { Table<Person>().map { p -> p.age.toFloat() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
  }
  "Long" - {
    "toInt" {
      val q = sql { Table<Person>().map { p -> p.age.toInt() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "toDouble" {
      val q = sql { Table<Person>().map { p -> p.age.toDouble() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "toFloat" {
      val q = sql { Table<Person>().map { p -> p.age.toFloat() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
  }
  "Float" - {
    "toInt" {
      val q = sql { Table<Person>().map { p -> p.age.toInt() } }
      shouldBeGolden(q.build<SqliteDialect>())
      println("hello")
    }
    "toLong" {
      val q = sql { Table<Person>().map { p -> p.age.toLong() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
    "toDouble" {
      val q = sql { Table<Person>().map { p -> p.age.toDouble() } }
      shouldBeGolden(q.build<SqliteDialect>())
    }
  }
})
