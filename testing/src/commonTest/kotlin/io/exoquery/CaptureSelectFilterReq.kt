@file:TracesEnabled(TraceType.SqlNormalizations::class, TraceType.Normalizations::class, TraceType.SqlQueryConstruct::class)

package io.exoquery

import io.exoquery.annotation.TracesEnabled
import io.exoquery.util.TraceType

class CaptureSelectFilterReq: GoldenSpecDynamic(CaptureSelectFilterReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "sql select.filter should expand correctly" - {
    "test query" {
      data class User(val id: Int, val name: String, val active: Int)
      data class Comment(val id: Int, val userId: Int, val createdAt: Long)
      data class UserCommentCount(val user: User, val commentCount: Int)

      val users = sql { Table<User>() }
      val comments = sql { Table<Comment>() }

      val now = sql.expression {
        free("now()")<Long>()
      }
      @SqlFragment
      fun IntervalDay(days: Int) = sql.expression {
        free("interval '$days days'")<Long>()
      }

      val q = sql {
        select {
          val u = from(users)
          val c = joinLeft(comments) { it.userId == u.id }
          where { u.active == 1 && c!!.createdAt > now.use - IntervalDay(30).use }
          groupBy(u)
          UserCommentCount(u, count(c!!.id))
        }.filter { uc -> uc.commentCount > 5 }
      }

      shouldBeGolden(q.xr, "XR")
      shouldBeGolden(q.buildPrettyFor.Postgres(), useTokenRendering = false)
    }

    data class Person(val id: Int, val name: String, val age: Int)
    data class Address(val ownerId: Int, val street: String, val city: String)

    "sql select.filter simple" {
      val people = sql { Table<Person>() }
      val addresses = sql { Table<Address>() }

      val c = sql {
        select {
          val p = from(people)
          where { p.age > 18 }
          p
        }.filter { ccc -> ccc.name == "Main St" }
      }
      shouldBeGolden(c.xr, "XR")
      shouldBeGolden(c.buildPrettyFor.Postgres(), useTokenRendering = false)
    }

    "sql select(where,groupBy).filter" {
      val people = sql { Table<Person>() }
      val addresses = sql { Table<Address>() }

      val c = sql {
        select {
          val p = from(people)
          val a = joinLeft(addresses) { it.ownerId == p.id }
          where { p.age > 18 }
          groupBy(p)
          p
        }.filter { ccc -> ccc.name == "Main St" }
      }
      shouldBeGolden(c.xr, "XR")
      shouldBeGolden(c.buildPrettyFor.Postgres(), useTokenRendering = false)
    }


    "sql select(where,groupBy).map" {
      val people = sql { Table<Person>() }
      val addresses = sql { Table<Address>() }

      val c = sql {
        select {
          val p = from(people)
          val a = joinLeft(addresses) { it.ownerId == p.id }
          where { p.age > 18 }
          groupBy(p)
          p
        }.map { ccc -> ccc.name to ccc.age }
      }
      shouldBeGolden(c.xr, "XR")
      shouldBeGolden(c.buildPrettyFor.Postgres(), useTokenRendering = false)
    }

  }
})
