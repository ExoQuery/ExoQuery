package io.exoquery.sample

import io.exoquery.SqlQuery
import io.exoquery.annotation.SqlFragment
import io.exoquery.sql

fun main() {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val ownerId: Int, val street: String, val zip: Int)
  data class Robot(val ownerId: Int, val model: String)


  @SqlFragment
  fun <T: Person> forUpdate(v: SqlQuery<T>) = sql {
    free("${v} FOR UPDATE").asPure<SqlQuery<T>>()
  }

  val q = sql {
    forUpdate(Table<Person>().filter { p -> p.age > 21 })
  }.dyanmic()


  println(q.buildFor.Postgres().value)

  // TODO put this into a unit test
  //val q = sql.select {
  //  val p = from(Table<Person>())
  //  val a = join(Table<Address>()) { a -> p.id == a.ownerId }
  //  val r = join(Table<Robot>()) { r -> p.id == r.ownerId }
  //  Triple(p, a, r)
  //}
  //println(q.normalizeSelects().xr.show())
  //
  //  //> Table(Person).flatMap { p ->
  //  //>   Table(Address).join { a -> p.id == a.ownerId }.flatMap {
  //  //>     a -> Table(Robot).join { r -> p.id == r.ownerId }.map { r ->
  //  //>       Triple(first = p, second = a, third = r)
  //  //>     }
  //  //>   }
  //  //> }
  //
  //
  //val q2 = sql {
  //  Table<Person>().flatMap { p ->
  //    internal.flatJoin(Table<Address>()) { a -> p.id == a.ownerId }.flatMap { a ->
  //      internal.flatJoin(Table<Robot>()) { r -> p.id == r.ownerId }.map { r ->
  //        Triple(p, a, r)
  //      }
  //    }
  //  }
  //}
  //println(q2.show())
  //
  //// Table(Person).flatMap { p ->
  ////   Table(Address).join { a -> p.id == a.ownerId }.flatMap { a ->
  ////     Table(Robot).join { r -> p.id == r.ownerId }.map { r ->
  ////       Triple(first = p, second = a, third = r)
  ////     }
  ////   }
  //// }

  //println("----------------- XR -----------------\n${filteredPeople.show()}")
  //println("--------------------- SQL ------------------\n" + pprintMisc(filteredPeople.build<PostgresDialect>()))
}
