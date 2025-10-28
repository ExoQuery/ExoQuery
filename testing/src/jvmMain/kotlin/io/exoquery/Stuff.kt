import io.exoquery.sql
import io.exoquery.PostgresDialect

////@file:TracesEnabled(TraceType.SqlNormalizations::class, TraceType.Normalizations::class)
//
//package io.exoquery
//
//import io.exoquery.annotation.CapturedFunction
//
//
//fun blah() {
//
//}
//
//data class Stuff(val x: Int, val y: String)
//
//// Note that "Build" -> "Rebuild" will work for this file because it is "JVM" specifically. It will not work for stuff in commonMain/Test
//// in general the "Build" -> "Rebuild" only works for platform specific targets
//fun main() {
//  // DO NOT move these out to the top level. The compiler will not will cause strange shadowing errors with commonTest/io.exoquery.testdata.TestData.kt classes
//  data class Person(val id: Int, val name: String, val age: Int)
//  data class Address(val ownerId: Int, val street: String, val zip: Int)
//
//  val joes = sql { Table<Person>().filter { p -> p.name == param("joe") } }
//
//  // TODO introduce a spec for these
//
//  @CapturedFunction
//  fun isJoe(name: String) =
//    sql.expression { name == "joe" }
//
//  val result = sql {
//    joes.filter { isJoe(it.name).use }.map { it.name }
//  }
//
//
////  @CapturedFunction
////  fun <T> joinPeopleToAddress(people: SqlQuery<T>, otherValue: String, f: (T) -> Int) =
////    sql.select {
////      val p = from(people)
////      val a = join(Table<Address>()) { a -> a.ownerId == f(p) && a.street == otherValue } // should have a verification that param(otherValue) fails
////      p to a
////    }
////
////  val r = "foobar"
////  val result = sql {
////    joinPeopleToAddress(joes, param(r)) { it.id }.map { kv -> kv.first.name to kv.second.street }
////  }
//
//  //println("-------- Result ------\n${result.params.lifts.map { it.showValue() }}")
//
//
////  @CapturedFunction
////  fun <T> joinPeopleToAddress(people: SqlQuery<T>, f: (T, Address) -> Boolean) =
////    sql.select {
////      val p = from(people)
////      val a = join(Table<Address>()) { a -> f(p, a) }
////      p to a
////    }
////
////  val joinFunction = sql.expression {
////    { p: Person, a: Address -> p.id == a.ownerId }
////  }
////  // TODO captured-function for SqlExpression
////
////  val result = sql {
////    joinPeopleToAddress(joes) { p, a -> joinFunction.use(p, a) /*p.id == a.ownerId*/ }.map { kv -> kv.first.name + " lives at " + kv.second.street }
////  }
//
//  //println(result.buildPretty<PostgresDialect>().params.map { it.showValue() })
//
//  println(result.buildPretty<PostgresDialect>())
//
//
//
//  // then try this only with a fun and generic
////  val joinPeopleToAddress2 = captureValue {
////     { input: SqlQuery<Person> ->
////       select {
////         val p = from(input)
////         // TODO now we should parse the IrGetValue. Can we found out that it is coming from captureValue? Print-debug the owernship chain
////         val a = join(Table<Address>()) { a -> a.ownerId == p.id }
////         p to a
////       }
////     }
////   }
////  println("hello")
////
////  val x = sql {
////    joinPeopleToAddress2.use.invoke(joes)
////  }
////  println(x.buildPretty(PostgresDialect()).value) // helloo
////
////
////  println("hello")
////
////  println("---------------------\n" + x.xr.show())
////  println("------------------ Reduced: ------------------\n" + BetaReduction(x.xr))
//
//
////  fun joinPeopleToAddress(people: @CapturedDynamic SqlQuery<Person>) =
////    select {
////      val p = from(people)
////      val a = join(Table<Address>()) { a -> a.ownerId == p.id }
////      p to a
////    }
////
////  //val result = joinPeopleToAddress2.use(jacks)
////  val result = joinPeopleToAddress(jacks)
////  println(result.buildPretty(PostgresDialect()).value)
//}
//
//
////class Ctx {
////  fun stuff(): String = "123"
////}
////fun taker(f: Ctx.() -> String) {
////  val ctx = Ctx()
////  println(ctx.f())
////}
////
////fun stuff(): Int = 456
////
////fun blah() {
////
////  taker {
////    val x = stuff()
////    x
////  }
////}

fun main() {
  data class Name(val first: String, val last: String)
  data class Person(val name: Name?, val age: Int)

  val p = Person(Name("Joe", "Smith"), 123)
  fun foo(v: String) = v
  fun foo(v: Name) = v

//  val s = printSource {
//    //(p.name ?: Name("John", "Doe")).first
//    foo(p.name?.first ?: "John")
//  }
//  println(s)

  val q = sql {
    Table<Person>().map { p -> p.name?.first ?: "John" }
  }
  println(q.build<PostgresDialect>())
}
