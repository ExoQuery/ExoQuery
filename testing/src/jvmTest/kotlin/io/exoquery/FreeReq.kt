package io.exoquery

import io.exoquery.sql.PostgresDialect
import io.exoquery.testdata.Person

class FreeReq: GoldenSpecDynamic(FreeReqGoldenDynamic, Mode.ExoGoldenTest(), {

  // TODO if 'free' not follorwed by anything there should be some kidn of compile error
  "static free" - {
    "simple sql function" {
      shouldBeGolden(capture { Table<Person>().filter { p -> free("MyFunction(${p.age})")<Boolean>() } }.build<PostgresDialect>())
    }
    "simple sql function - pure" {
      shouldBeGolden(capture { Table<Person>().filter { p -> free("MyFunction(${p.age})").asPure<Boolean>() } }.build<PostgresDialect>())
    }
    "simple sql function - condition" {
      shouldBeGolden(capture { Table<Person>().filter { p -> free("MyFunction(${p.age})").asConditon() } }.build<PostgresDialect>())
    }
    "simple sql function - condition" {
      shouldBeGolden(capture { Table<Person>().filter { p -> free("MyFunction(${p.age})").asPureConditon() } }.build<PostgresDialect>())
    }
  }

  "query with free" - {
    "static query in free" {
      val query = capture {
        Table<Person>().filter { p -> p.name == "Joe" }
      }
      val free = capture {
        free("beforeStuff() ${query} afterStuff()").asPure<SqlAction<Person, Long>>()
      }
      shouldBeGolden(free.build<PostgresDialect>())
    }

    "dynamic query in free" {
      val query = capture {
        Table<Person>().filter { p -> p.name == "Joe" }
      }.dyanmic()

      val free = capture {
        free("beforeStuff() ${query} afterStuff()").asPure<SqlAction<Person, Long>>()
      }
      shouldBeGolden(free.build<PostgresDialect>())
    }

    "direct query in free" {
      val free = capture {
        free("beforeStuff() ${Table<Person>().filter { p -> p.name == "Joe" }} afterStuff()").asPure<SqlAction<Person, Long>>()
      }
      shouldBeGolden(free.build<PostgresDialect>())
    }
  }

  "action with free" - {
    "static action in free" {
      val action = capture {
        insert<Person> { setParams(Person(1, "Joe", 123)) }
      }
      val free = capture {
        free("beforeStuff() ${action} afterStuff()").asPure<SqlAction<Person, Long>>()
      }
      shouldBeGolden(free.build<PostgresDialect>().determinizeDynamics())
    }
    "dynamic action in free" {
      val action = capture {
        insert<Person> { setParams(Person(1, "Joe", 123)) }
      }.dyanmic()

      val free = capture {
        free("beforeStuff() ${action} afterStuff()").asPure<SqlAction<Person, Long>>()
      }
      shouldBeGolden(free.build<PostgresDialect>().determinizeDynamics())
    }
    "direct action in free" {
      val free = capture {
        free(
          "beforeStuff() ${
            insert<Person> {
              setParams(
                Person(
                  1,
                  "Joe",
                  123
                )
              )
            }
          } afterStuff()"
        ).asPure<SqlAction<Person, Long>>()
      }
      shouldBeGolden(free.build<PostgresDialect>().determinizeDynamics())
    }
    "whole action in free" {
      val action = capture {
        free(
          """
            CREATE TABLE Launch (
                flightNumber INTEGER NOT NULL,
                missionName TEXT NOT NULL,
                details TEXT,
                launchSuccess INTEGER DEFAULT NULL,
                launchDateUTC TEXT NOT NULL,
                patchUrlSmall TEXT,
                patchUrlLarge TEXT,
                articleUrl TEXT
            )
          """
        ).asPure<SqlAction<Nothing, Long>>()
      }
      shouldBeGolden(action.build<PostgresDialect>().determinizeDynamics())
    }
  }

})
