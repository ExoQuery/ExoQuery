package io.exoquery

class NestedSelectClauseMistake: MessageSpecDynamic(NestedSelectClauseMistakeGoldenDynamic, Mode.ExoGoldenTest(), {

  "should report error for nested select clause mistake - unit error" {
    shouldBeGoldenError(
      """
      import io.exoquery.*
      data class Person(val id: Int, val name: String, val age: Int)
      data class Address(val id: Int, val personId: Int, val city: String)
       fun run() {
        val inner = sql.select {
          val queryA = sql.select {
            val p = from(Table<Person>())
            val a = join(Table<Address>()) { a -> a.personId == p.id }
            Triple(p.name, p.age, a.city)
          }
          val queryB = sql { queryA.filter { t -> t.third == "New York" } }          
        }
        println(inner.buildFor.Postgres().value)
      }
      """.trimIndent()
    )
  }

  "should report error for nested select clause mistake - invalid functions inside" {
    shouldBeGoldenError(
      """
      import io.exoquery.*
      data class Person(val id: Int, val name: String, val age: Int)
      data class Address(val id: Int, val personId: Int, val city: String)
       fun run() {
        val inner = sql.select {
          val queryA = sql.select {
            val p = from(Table<Person>())
            val a = join(Table<Address>()) { a -> a.personId == p.id }
            Triple(p.name, p.age, a.city)
          }
          val queryB = sql { queryA.filter { t -> t.third == "New York" } }
          // invalid functions that need to be OUTSIDE of the sql.select block
          println("This is an invalid function inside the sql.select block")
        }
        println(inner.buildFor.Postgres().value)
      }
      """.trimIndent()
    )
  }
})
