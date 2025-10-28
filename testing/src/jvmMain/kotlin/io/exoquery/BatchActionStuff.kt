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
  val peopleSeq =
    sequenceOf(
      PersonSimple(1, "Joe", 123),
      PersonSimple(2, "Jane", 456),
      PersonSimple(3, "Jack", 789),
      PersonSimple(4, "Jill", 101112),
      PersonSimple(5, "Mack", 101112),
      PersonSimple(6, "Mick", 101112),
      PersonSimple(7, "Muck", 101112),
      PersonSimple(8, "Muck", 101112)
    )

  // TODO when using setParams why the heck does it use a contextual serializer for the String??? Need to test for that case (even the regular actions should test for that case!)
  val s =
    sql.batch(peopleSeq) { p ->
      //insert<Person> { set(name.first to param(n.first), age to 123) }.returning { p -> Name(p.name.first + "-stuff", p.name.last + "-otherStuff") }
      // TODO Odd error need to figure out: Exception in thread "main" java.lang.NoClassDefFoundError: I
      insert<PersonSimple> { setParams(p) } //.returning { p -> Name(p.name.first + "-stuff", p.name.last + "-otherStuff") }
      //insert<PersonSimple> { set(id to param(p.id), name to param(p.name), age to param(p.age)) } //.returningKeys { name to age }//.returningKeys()
      //insert<PersonSimple> { setParams(p) }.returningKeys { name to age }//.returningKeys()
      //insert<PersonSimple> { set(name to "Joe", age to 123) }
    }


  //println(s)


  // TODO now need to run through the sequence in the CompiledBatchAction and make the BatchGroup instances (i.e. by applying list elements on the refiner)
  println(s.build<PostgresDialect>()) //.produceBatchGroups().map { it.effectiveToken().renderWith(Renderer()) }.toList()

  //val build = s.build<PostgresDialect>()
  //println(build.show())

}
