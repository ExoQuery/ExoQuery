package io.exoquery

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.exoquery.plugin.Registrar
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
class PluginCompileTest : StringSpec({
//  "should give correct error about un-parsable type" {
//    val source = SourceFile.kotlin(
//      "Sample.kt",
//      """
//      package sample
//
//      import io.exoquery.*
//
//      class MyType(val value: Int)
//
//      data class MyPerson(val name: String, val age: MyType)
//      //fun personName(p: Person) = p.name
//      fun run() {
//        val q = sql { Table<MyPerson>() }
//        println(q.buildFor.Postgres().value)
//      }
//
//
//      """.trimIndent()
//    )
//
//    val result = KotlinCompilation().apply {
//      sources = listOf(source)
//      inheritClassPath = true
//      messageOutputStream = System.out // see diagnostics
//      compilerPluginRegistrars = listOf(Registrar())
//      // k2 = true
//    }.compile()
//
//    println("---------- Messages ----------\n${result.messages}\n------------------------------")
//
//    result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
//    result.messages shouldContain("ERROR Could not parse type from: MyType")
//  }
  "should give correct error about un-parsable type" {
    val source = SourceFile.kotlin(
      "Sample_BadSqlFunction.kt",
      """
    package sample
    import io.exoquery.*

    data class MyPerson(val name: String, val age: Int)
    fun nameAndSuffix(name: String) = name + "_suffix"

    fun run() {
      val q = sql { Table<MyPerson>().filter { p -> nameAndSuffix(p.name) == "Joe" } }
      println(q.buildFor.Postgres().value)
    }


    """.trimIndent()
    )

    // Use jars produced by :testing-compile-dependencies:copyDependencies as the compilation classpath
    val depsDir = File("../testing-compile-dependencies/target/dependencies")
    val requestedJars: List<File> =
      if (depsDir.exists()) depsDir.listFiles { f -> f.isFile && f.name.endsWith(".jar") }?.sortedBy { it.name }?.toList().orEmpty()
      else emptyList()
    val outStream = ByteArrayOutputStream()

    val result = KotlinCompilation().apply {
      sources = listOf(source)
      inheritClassPath = true
      messageOutputStream = outStream // see diagnostics
      compilerPluginRegistrars = listOf(Registrar())
      // k2 = true
    }.compile()

    println("---------- Messages ----------\n${result.messages}\n------------------------------")

    result.exitCode shouldBe KotlinCompilation.ExitCode.OK

  }
})
