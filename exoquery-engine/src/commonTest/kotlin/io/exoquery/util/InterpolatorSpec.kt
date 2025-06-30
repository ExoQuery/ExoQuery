//package io.exoquery.util
//
//import io.exoquery.kmp.pprint.PPrinter
//import io.exoquery.pprint.PPrinterConfig
//import io.exoquery.pprint.Tree
//import io.kotest.core.spec.style.FreeSpec
//import io.kotest.matchers.shouldBe
//import kotlinx.serialization.Serializable
//
//// NOTE Don't want to move this to the `testing` project for now because this test is reliant on the Terpal plugin
//// and we don't want to be forced to add that as a dependency to the `testing` project
//class InterpolatorSpec : FreeSpec({
//  val interp = Tracer(TraceType.Standard, TraceConfig.empty, defaultIndent = 0, color = false, globalTracesEnabled = { true })
//
//  // TODO implement a Show class that the PrintXR in the tracer will use
//  //      actually use PrintMisc
//  @Serializable
//  data class Small(val id: Int) : ShowTree {
//    override fun showTree(config: PPrinterConfig): Tree = PPrinter<Small>(Small.serializer(), config).treeify(this, null, true, false)
//  }
//
//  val small = Small(123)
//
//  "traces small objects on single line - single" {
//    interp("small object: $small").generateString() shouldBe ("small object: Small(123) " to 0)
//  }
//
//  @Serializable
//  data class Large(
//    val id: Int,
//    val one: String,
//    val two: String,
//    val three: String,
//    val four: String,
//    val five: String,
//    val six: String,
//    val seven: String,
//    val eight: String,
//    val nine: String,
//    val ten: String
//  ) : ShowTree {
//    override fun showTree(config: PPrinterConfig): Tree = PPrinter<Large>(Large.serializer(), config).treeify(this, null, true, false)
//  }
//
//  var vars = (0 until 10).map { i -> (0 until i * 2).map { _ -> "Test" }.joinToString("") }.toList()
//  val large = Large(123, vars[0], vars[1], vars[2], vars[3], vars[4], vars[5], vars[6], vars[7], vars[8], vars[9])
//
//  "traces large objects on multiple line - single" {
//    interp("large object: $large").generateString() shouldBe ((
//        """large object:
//        ||  Large(
//        ||    123,
//        ||    "",
//        ||    "TestTest",
//        ||    "TestTestTestTest",
//        ||    "TestTestTestTestTestTest",
//        ||    "TestTestTestTestTestTestTestTest",
//        ||    "TestTestTestTestTestTestTestTestTestTest",
//        ||    "TestTestTestTestTestTestTestTestTestTestTestTest",
//        ||    "TestTestTestTestTestTestTestTestTestTestTestTestTestTest",
//        ||    "TestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTest",
//        ||    "TestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTestTest"
//        ||  )
//        |""".trimMargin() to 0
//        ))
//  }
//
//})
