package io.exoquery

object ReductionRepo {
  data class Person(val id: Int, val name: String, val age: Int)
  data class Address(val id: Int, val personId: Int, val city: String)
  val queryA = sql.select {
    val p = from(Table<Person>())
    val a = join(Table<Address>()) { a -> a.personId == p.id }
    where { p.name == "Joe" }
    Triple(p.name, p.age, a.city)
  }
  val queryB = sql { queryA.filter { t -> t.third == "NewYork" } }.dynamic()
}

fun main() {
  val reducedQuery = ReductionRepo.queryB
  println(reducedQuery.build<PostgresDialect>().value)
}
