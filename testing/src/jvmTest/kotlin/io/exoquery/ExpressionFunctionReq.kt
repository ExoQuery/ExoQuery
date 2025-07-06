package io.exoquery

import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Person

class ExpressionFunctionReq : GoldenSpecDynamic(ExpressionFunctionReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "String" - {
    "toInt" {
      val q = capture { Table<Person>().map { p -> p.name.toInt() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toLong" {
      val q = capture { Table<Person>().map { p -> p.name.toLong() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toBoolean" {
      val q = capture { Table<Person>().map { p -> p.name.toBoolean() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "casting" {
      val q = capture { Table<Person>().map { p -> p.name + (p.age as String) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "string concat" {
      val q = capture { Table<Person>().map { p -> p.name + " " + p.name } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "substr - regular" {
      val q = capture { Table<Person>().map { p -> p.name.substring(1, 2) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "substr" {
      val q = capture { Table<Person>().map { p -> p.name.sql.substring(1, 2) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "left" {
      val q = capture { Table<Person>().map { p -> p.name.sql.left(2) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "right" {
      val q = capture { Table<Person>().map { p -> p.name.sql.right(2) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "replace" {
      val q = capture { Table<Person>().map { p -> p.name.sql.replace("a", "b") } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
// TODO method whitelist for this case
//    "upper" {
//      val q = capture { Table<Person>().map { p -> p.name.uppercase() } }
//      shouldBeGolden(q.build<PostgresDialect>())
//    }
    "upper - sql" {
      val q = capture { Table<Person>().map { p -> p.name.sql.uppercase() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
// TODO method whitelist for this case
//    "lower" {
//      val q = capture { Table<Person>().map { p -> p.name.lowercase() } }
//      shouldBeGolden(q.build<PostgresDialect>())
//    }
    "lower - sql" {
      val q = capture { Table<Person>().map { p -> p.name.sql.lowercase() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
  "StringInterpolation" - {
    "multiple elements" {
      val q = capture { Table<Person>().map { p -> "${p.name}-${p.age}" } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "single element" {
      val q = capture { Table<Person>().map { p -> "${p.name}" } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "single constant" {
      val q = capture { Table<Person>().map { p -> "${"constant"}" } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "many elements" {
      data class LotsOfFields(
        val name: String,
        val age: Int,
        val address: String,
        val phone: String
      )
      val q = capture { Table<LotsOfFields>().map { l ->
        "${l.name}-foo-${l.age}-bar-${l.address}-baz-${l.phone}-blin"
      } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
  "Int" - {
    "toLong" {
      val q = capture { Table<Person>().map { p -> p.age.toLong() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toDouble" {
      val q = capture { Table<Person>().map { p -> p.age.toDouble() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toFloat" {
      val q = capture { Table<Person>().map { p -> p.age.toFloat() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
  "Long" - {
    "toInt" {
      val q = capture { Table<Person>().map { p -> p.age.toInt() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toDouble" {
      val q = capture { Table<Person>().map { p -> p.age.toDouble() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toFloat" {
      val q = capture { Table<Person>().map { p -> p.age.toFloat() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
  "Float" - {
    "toInt" {
      val q = capture { Table<Person>().map { p -> p.age.toInt() } }
      shouldBeGolden(q.build<PostgresDialect>())
      println("hello")
    }
    "toLong" {
      val q = capture { Table<Person>().map { p -> p.age.toLong() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toDouble" {
      val q = capture { Table<Person>().map { p -> p.age.toDouble() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
})
