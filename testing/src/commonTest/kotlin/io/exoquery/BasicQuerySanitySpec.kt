package io.exoquery

import io.exoquery.PostgresDialect

// Note that the 1st time you overwrite the golden file it will still fail because the compile is using teh old version
// Also note that it won't actually override the BasicQuerySanitySpecGolden file unless you change this one

// build the file BasicQuerySanitySpecGolden.kt, is that as the constructor arg
class BasicQuerySanitySpec : GoldenSpecDynamic(BasicQuerySanitySpecGoldenDynamic, Mode.ExoGoldenTest(), {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val ownerId: Int, val street: String, val city: String)

//  val p: Person = TODO()
//
//  class Value {
//    operator fun set(value: Any) = TODO()
//  }

//  fun <T> setValues(f: (T).(Value) -> set) = TODO()
//
//  setValues<Person> { set(it[name] = "Joe", it[age] = 123) }
//
//  interface SetValues
//
//  interface CapBlock {
//    val set: Value
//    fun <T> insert(f: (T).() -> set) = TODO()
//    fun <T> insertValue(value: T) = TODO()
//
//    fun set(vararg values: Pair<Any, Any>): SetValues = TODO()
//
//
//    fun <T> param(value: T): T = TODO()
//  }
//
//  fun <T> myCapture(block: CapBlock.() -> SetValues): Int = TODO()
//
//  fun foo() {
//    val a = "Joe"
//    val b = 123
//    val p = Person(1, a, b)
//    myCapture {
//      insert<Person> { set(name to param(joe), age to param(b)) }
//      // .excludingColumns(name, age)
//      // TODO should should have docs about how it does a "quiet" return (and about dialect support)
//      // .returningColumns(id, name)
//      // TODO should have note about how this necessarily introduces a "returning" statement to the SQL (and about dialect support)
//      // .returning { p -> something(p.name) }
//    }
//  }


  "basic query" {
    val people = sql { Table<Person>() }
    people.build<PostgresDialect>("basic query").shouldBeGolden()
  }
  "query with map" {
    val people = sql { Table<Person>().map { p -> p.name } }
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with filter" {
    val people = sql { Table<Person>().filter { p -> p.age > 18 } }
    shouldBeGolden(people.build<PostgresDialect>())
  }
  "query with flatMap" {
    val people = sql { Table<Person>().flatMap { p -> Table<Address>().filter { a -> a.ownerId == p.id } } }
    shouldBeGolden(people.build<PostgresDialect>())
  }
})
