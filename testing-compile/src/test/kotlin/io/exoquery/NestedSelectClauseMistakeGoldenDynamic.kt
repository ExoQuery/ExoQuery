package io.exoquery

import io.exoquery.printing.GoldenResult
import io.exoquery.printing.cr
import io.exoquery.printing.kt
import io.exoquery.printing.pl

object NestedSelectClauseMistakeGoldenDynamic: MessageSpecFile {
  override val messages = mapOf<String, GoldenResult>(
    "should report error for nested select clause mistake - unit error" to pl(
      """
      [ExoQuery] Could not understand an expression or query due to an error: Nothing was returned from the sql.select clause. A sql.select clause must return a value, usually a primitive or instance of a data class.
      ------------ Source ------------
      {
          val queryA = sql.select {
            val p = from(Table<Person>())
            val a = join(Table<Address>()) { a -> a.personId == p.id }
            Triple(p.name, p.age, a.city)
          }
          val queryB = sql { queryA.filter { t -> t.third == "New York" } }          
        }
      ------------ Raw Expression ------------
      local fun SelectClauseCapturedBlock.<anonymous>() {
        val queryA: @Captured SqlQuery<Triple<String, Int, String>> = Companion.fromPackedXR</* null */>(select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; Triple(first = p.name, second = p.age, third = a.city) }), runtimes = Companion.<get-Empty>(), params = ParamSet(lifts = listOf<Param<T>>(elements = [])))
        val queryB: @Captured SqlQuery<Triple<String, Int, String>> = Companion.fromPackedXR</* null */>(select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; Triple(first = p.name, second = p.age, third = a.city) }.filter { t -> t.third == New York }), runtimes = Companion.<get-Empty>(), params = ParamSet(lifts = listOf<Param<T>>(elements = [])).plus(other = Companion.fromPackedXR</* null */>(select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; Triple(first = p.name, second = p.age, third = a.city) }), runtimes = Companion.<get-Empty>(), params = queryA.<get-params>()).<get-params>()))
      }
      
      """
    ),
    "should report error for nested select clause mistake - invalid functions inside" to pl(
      """
      [ExoQuery] Could not understand an expression or query due to an error: Nothing was returned from the sql.select clause. A sql.select clause must return a value, usually a primitive or instance of a data class.
      ------------ Source ------------
      {
          val queryA = sql.select {
            val p = from(Table<Person>())
            val a = join(Table<Address>()) { a -> a.personId == p.id }
            Triple(p.name, p.age, a.city)
          }
          val queryB = sql { queryA.filter { t -> t.third == "New York" } }
          // invalid functions that need to be OUTSIDE of the sql.select block
          println("This is an invalid function inside the sql.select block")
        }
      ------------ Raw Expression ------------
      local fun SelectClauseCapturedBlock.<anonymous>() {
        val queryA: @Captured SqlQuery<Triple<String, Int, String>> = Companion.fromPackedXR</* null */>(select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; Triple(first = p.name, second = p.age, third = a.city) }), runtimes = Companion.<get-Empty>(), params = ParamSet(lifts = listOf<Param<T>>(elements = [])))
        val queryB: @Captured SqlQuery<Triple<String, Int, String>> = Companion.fromPackedXR</* null */>(select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; Triple(first = p.name, second = p.age, third = a.city) }.filter { t -> t.third == New York }), runtimes = Companion.<get-Empty>(), params = ParamSet(lifts = listOf<Param<T>>(elements = [])).plus(other = Companion.fromPackedXR</* null */>(select { val p = from(Table(Person)); val a = join(Table(Address)) { a.personId == p.id }; Triple(first = p.name, second = p.age, third = a.city) }), runtimes = Companion.<get-Empty>(), params = queryA.<get-params>()).<get-params>()))
        println(message = "This is an invalid function inside the sql.select block")
      }
      
      """
    ),
  )
}
