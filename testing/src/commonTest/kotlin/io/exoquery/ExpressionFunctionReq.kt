package io.exoquery

import io.exoquery.PostgresDialect
import io.exoquery.testdata.Person

class ExpressionFunctionReq : GoldenSpecDynamic(ExpressionFunctionReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "String" - {
    "toInt" {
      val q = sql { Table<Person>().map { p -> p.name.toInt() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toLong" {
      val q = sql { Table<Person>().map { p -> p.name.toLong() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toBoolean" {
      val q = sql { Table<Person>().map { p -> p.name.toBoolean() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "casting" {
      val q = sql { Table<Person>().map { p -> p.name + (p.age as String) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "string concat" {
      val q = sql { Table<Person>().map { p -> p.name + " " + p.name } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "substr - regular" {
      val q = sql { Table<Person>().map { p -> p.name.substring(1, 2) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "substr" {
      val q = sql { Table<Person>().map { p -> p.name.sql.substring(1, 2) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "left" {
      val q = sql { Table<Person>().map { p -> p.name.sql.left(2) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "right" {
      val q = sql { Table<Person>().map { p -> p.name.sql.right(2) } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "replace" {
      val q = sql { Table<Person>().map { p -> p.name.sql.replace("a", "b") } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
// TODO method whitelist for this case
//    "upper" {
//      val q = sql { Table<Person>().map { p -> p.name.uppercase() } }
//      shouldBeGolden(q.build<PostgresDialect>())
//    }
    "upper - sql" {
      val q = sql { Table<Person>().map { p -> p.name.sql.uppercase() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
// TODO method whitelist for this case
//    "lower" {
//      val q = sql { Table<Person>().map { p -> p.name.lowercase() } }
//      shouldBeGolden(q.build<PostgresDialect>())
//    }
    "lower - sql" {
      val q = sql { Table<Person>().map { p -> p.name.sql.lowercase() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
  "StringInterpolation" - {
    "multiple elements" {
      val q = sql { Table<Person>().map { p -> "${p.name}-${p.age}" } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "single element" {
      val q = sql { Table<Person>().map { p -> "${p.name}" } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "single constant" {
      val q = sql { Table<Person>().map { p -> "${"constant"}" } }
      shouldBeGolden(q.build<PostgresDialect>())
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
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
  "Int" - {
    "toLong" {
      val q = sql { Table<Person>().map { p -> p.age.toLong() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toDouble" {
      val q = sql { Table<Person>().map { p -> p.age.toDouble() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toFloat" {
      val q = sql { Table<Person>().map { p -> p.age.toFloat() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
  "Long" - {
    "toInt" {
      val q = sql { Table<Person>().map { p -> p.age.toInt() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toDouble" {
      val q = sql { Table<Person>().map { p -> p.age.toDouble() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toFloat" {
      val q = sql { Table<Person>().map { p -> p.age.toFloat() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
  "Float" - {
    "toInt" {
      val q = sql { Table<Person>().map { p -> p.age.toInt() } }
      shouldBeGolden(q.build<PostgresDialect>())
      println("hello")
    }
    "toLong" {
      val q = sql { Table<Person>().map { p -> p.age.toLong() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
    "toDouble" {
      val q = sql { Table<Person>().map { p -> p.age.toDouble() } }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
})
