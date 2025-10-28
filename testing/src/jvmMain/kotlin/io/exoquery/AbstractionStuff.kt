package io.exoquery

//hello


//interface HasId { val id: Int }
//data class Person(override val id: Int, val name: String, val age: Int): HasId
//data class Martian(override val id: Int, val title: String): HasId
//data class Address(val ownerId: Int, val street: String, val zip: Int)
//
//fun main() {
//  @CapturedFunction
//  fun <T: HasId> myFunction(input: SqlQuery<T>, selector: (T) -> String) =
//    sql.select {
//      val p = from(input)
//      val a = join(Table<Address>()) { a -> a.ownerId == p.id }
//      selector(p) to a
//    }
//  val myQuery = sql {
//    val martians =
//      myFunction(Table<Martian>().filter { it.title == "Maartok" }) { m -> m.title }
//    val people =
//      myFunction(Table<Person>().filter { it.name == "Joe" }) { p -> p.name }
//    martians unionAll people
//  }
//  println(myQuery.buildPretty<PostgresDialect>().value)
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//// hello
