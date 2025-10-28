package io.exoquery

// This just doesn't work
//object Variant1 {
//
//  interface Values
//  interface Setter {
//    operator fun set(key: Any, value: Any): SetValue
//  }
//  interface SetValue
//
//  interface CaptureBlock {
//    fun <T> param(value: T): T
//    val set: Setter
//
//    fun values(vararg setter: Setter): Values
//
//    fun <T> insert(insertBlock: (T).() -> Values): T
//  }
//
//  fun sql(block: CaptureBlock.() -> Unit): Unit = TODO()
//
//
//  fun use() {
//    data class Person(val id: Int, val name: String, val age: Int)
//
//    sql {
//      // can't do it like this
//      insert<Person> { values(set[name] = "Joe") }
//    }
//
//  }
//}

val myName = "Joe"
val myAge = 123

object Variant2 {

  interface Values

  interface CaptureBlock {
    fun <T> param(value: T): T

    fun set(vararg setter: Pair<Any, Any>): Values

    fun <T> insert(insertBlock: (T).() -> Values): T
  }

  fun sql(block: CaptureBlock.() -> Unit): Unit = TODO()


  fun use() {
    data class Person(val id: Int, val name: String, val age: Int)

    // TODO write returningColumns after values function


    sql {
      insert<Person> { set(name to "Leah", age to 9) }
    }

  }
}


object Variant3 {


  fun use() {
    data class Person(val id: Int, val name: String, val age: Int)

    sql {
      insert<Person> {
        it[name] = "Leah"
        it[age] = 9
      }
    }

  }

  interface Values

  interface Setter {
    operator fun set(key: Any, value: Any): SetValue
  }

  interface SetValue

  interface CaptureBlock {
    fun <T> param(value: T): T

    fun <T> insert(insertBlock: (T).(Setter) -> Unit): T
  }

  fun sql(block: CaptureBlock.() -> Unit): Unit = TODO()


}
