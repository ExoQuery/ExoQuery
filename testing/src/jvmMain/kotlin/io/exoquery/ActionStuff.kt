package io.exoquery

import io.exoquery.PostgresDialect

fun main() {
  data class Name(val first: String, val last: String)
  data class Person(val id: Int, val name: Name, val age: Int)

  data class PersonSimple(val id: Int, val name: String, val age: Int)
  data class PersonSimple2(val name: String)
  data class PersonSimple3(val age: Int)

//  val output = printSourceBefore {
//    sql {
//      insert<Person> { set(name to "Joe", age to 123) }
//    }
//  }
//
//  println(output)

  // TODO need to implement parsing for `as` which should do a silent SQL cast (i.e. just ignore the `as X` part in the AST)

  val n = Name("Joe", "Smith")
  val p = Person(1, Name("joe", "smith"), 123)
  val ps = PersonSimple(1, "joe", 123)
  val ps2 = PersonSimple2("joe")
  val ps3 = PersonSimple3(123)

  val s =
    sql {
      //insert<Person> { set(name.first to param(n.first), age to 123) }.returning { p -> Name(p.name.first + "-stuff", p.name.last + "-otherStuff") }
      // TODO Odd error need to figure out: Exception in thread "main" java.lang.NoClassDefFoundError: I
      //insert<Person> { setParams(p) }.returning { p -> Name(p.name.first + "-stuff", p.name.last + "-otherStuff") }
      insert<Person> { setParams(p) }.returningKeys { name to age }//.returningKeys()
      //insert<PersonSimple> { set(name to "Joe", age to 123) }
    }
  // This is fine .xr, .params, .runtimes


  //println(s)

  println("--------------- XR ---------------\n${s.xr.show()}")
//  println("hellooooooooo")
  println(s.build<PostgresDialect>().show())

}
