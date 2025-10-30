package io.exoquery


//fun main() {
//  data class Person(val id: Int, val name: String, val age: Int)
//  data class Address(val ownerId: Int, val street: String, val city: String)
//  data class Robot(val ownerId: Int, val name: String, val model: String)
//
//
//  @SqlFunction
//  fun Person.joinAddress() = sql {
//    flatJoin(Table<Address>()) { a -> this@Person.id == a.ownerId }
//  }
//
//  @SqlFunction
//  fun Person.joinRobot() = sql {
//    flatJoin(Table<Robot>()) { r -> this@Person.id == r.ownerId }
//  }
//
//  val cap = sql.select {
//    val p = from(Table<Person>())
//    val a = from(p.joinAddress())
//    val r = from(p.joinRobot())
//    Triple(p, a, r)
//  }
//  println("----------------- XR ---------------\n" + cap.xr.showRaw())
//  val built = cap.buildPretty<PostgresDialect>()
//  println("----------------- SQL ---------------\n" + built.value)
//}


//fun main() {
//  data class Person(val id: Int, val name: String, val age: Int)
//  data class Address(val ownerId: Int, val street: String, val city: String)
//  data class Robot(val ownerId: Int, val name: String, val model: String)
//
//
//  @SqlFunction
//  fun Person.joinAddress() = sql {
//    flatJoin(Table<Address>()) { a -> this@Person.id == a.ownerId }
//  }
//
//  @SqlFunction
//  fun Person.joinRobot() = sql {
//    flatJoin(Table<Robot>()) { r -> this@Person.id == r.ownerId }
//  }
//
//  val cap = sql.select {
//    val p = from(Table<Person>())
//    val a = from(p.joinAddress())
//    val r = from(p.joinRobot())
//    Triple(p, a, r)
//  }
//  println("----------------- XR ---------------\n" + cap.xr.showRaw())
//  val built = cap.buildPretty<PostgresDialect>()
//  println("----------------- SQL ---------------\n" + built.value)
//}
