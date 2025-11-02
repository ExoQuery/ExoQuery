package io.exoquery

import com.tschuchort.compiletesting.SourceFile

// A Req-style spec that uses MessageSpecDynamic instead of GoldenSpecDynamic
class PluginCompileReq : MessageSpecDynamic(PluginCompileReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "should report error parsing external function" {
    shouldBeGoldenError(
      """
      import io.exoquery.*
      data class MyPerson(val name: String, val age: Int)
      fun externalFunction(name: String) = name + "_suffix"

      fun run() {
        val q = sql { Table<MyPerson>().filter { p -> externalFunction(p.name) == "Joe" } }
        println(q.buildFor.Postgres().value)
      }
      """.trimIndent()
    )
  }

  "should report error parsing external function - error details enabled" {
    shouldBeGoldenError(
      """
      @file:io.exoquery.annotation.ErrorDetailsEnabled(false)
      package sample
      import io.exoquery.*
      data class MyPerson(val name: String, val age: Int)
      fun externalFunction(name: String) = name + "_suffix"

      fun run() {
        val q = sql { Table<MyPerson>().filter { p -> externalFunction(p.name) == "Joe" } }
        println(q.buildFor.Postgres().value)
      }
      """.trimIndent()
    )
  }
  "should report error parsing external function - error details enabled, custom stack count" {
    shouldBeGoldenError(
      """
        @file:io.exoquery.annotation.ErrorDetailsEnabled(false, 5)
        package sample
        import io.exoquery.*
        data class MyPerson(val name: String, val age: Int)
        fun externalFunction(name: String) = name + "_suffix"

        fun run() {
          val q = sql { Table<MyPerson>().filter { p -> externalFunction(p.name) == "Joe" } }
          println(q.buildFor.Postgres().value)
        }
        """.trimIndent()
    )
  }
})
