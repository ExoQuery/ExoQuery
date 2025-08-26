package io.exoquery

import io.exoquery.annotation.ExoEntity
import io.exoquery.annotation.ExoField
import io.exoquery.sql.PostgresDialect

class NamingAnnotationActionReq: GoldenSpecDynamic(NamingAnnotationActionReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "action with rename" - {
    @ExoEntity("PERSON") data class Person(@ExoField("ID") val id: Int, @ExoField("NAME") val name: String, val age: Int)

    "should work (and quote) in insert" {
      val q = capture {
        insert<Person> { set(name to "Joe", age to 123) }.returning { p -> p.id to p.name }
      }.dyanmic()
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "should work (and quote) in insert using self column" {
      val q = capture {
        insert<Person> { set(name to name + "_Suffix", age to 123) }.returning { p -> p.id to p.name }
      }.dyanmic()
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "should work (and quote) in insert using self column with onConflict ignore" {
      val q = capture {
        insert<Person> { set(name to "Joe", age to 123).onConflictIgnore(id) }
      }
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "should work (and quote) in insert using self column with onConflict update" {
      val q = capture {
        insert<Person> { set(name to "Joe", age to 123).onConflictUpdate(id) { excl -> set(name to excl.name + "_Suffix") } }
      }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }

  "nested action with rename" - {
    data class Name(@ExoField("FIRST") val first: String, val last: String)
    @ExoEntity("PERSON")
    data class Person(@ExoField("ID") val id: Int, val name: Name, val age: Int)

    "should work (and quote) in insert" {
      val q = capture {
        insert<Person> { set(name.first to "Joe", age to 123) }.returning { p -> p.id to p.name }
      }.dyanmic()
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "should work (and quote) in insert using self column" {
      val q = capture {
        insert<Person> { set(name.first to name.first + "_Suffix", age to 123) }.returning { p -> p.id to p.name }
      }.dyanmic()
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "should work (and quote) in insert using self column with onConflict ignore" {
      val q = capture {
        insert<Person> { set(name.first to "Joe", age to 123).onConflictIgnore(name.first) }
      }
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "should work (and quote) in insert using self column with onConflict update" {
      val q = capture {
        insert<Person> { set(name.first to "Joe", age to 123).onConflictUpdate(id) { excl -> set(name.first to excl.name.first + "_Suffix") } }
      }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }

  "nested action with rename - intermediates renames should not matter" - {
    @ExoEntity("NAME") data class Name(@ExoField("FIRST") val first: String, val last: String)
    @ExoEntity("PERSON") data class Person(@ExoField("ID") val id: Int, @ExoField("NAME") val name: Name, val age: Int)

    "should work (and quote) in insert" {
      val q = capture {
        insert<Person> { set(name.first to "Joe", age to 123) }.returning { p -> p.id to p.name }
      }.dyanmic()
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "should work (and quote) in insert using self column" {
      val q = capture {
        insert<Person> { set(name.first to name.first + "_Suffix", age to 123) }.returning { p -> p.id to p.name }
      }.dyanmic()
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "should work (and quote) in insert using self column with onConflict ignore" {
      val q = capture {
        insert<Person> { set(name.first to "Joe", age to 123).onConflictIgnore(name.first) }
      }
      shouldBeGolden(q.build<PostgresDialect>())
    }

    "should work (and quote) in insert using self column with onConflict update" {
      val q = capture {
        insert<Person> { set(name.first to "Joe", age to 123).onConflictUpdate(id) { excl -> set(name.first to excl.name.first + "_Suffix") } }
      }
      shouldBeGolden(q.build<PostgresDialect>())
    }
  }
})
