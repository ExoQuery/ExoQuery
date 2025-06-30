//package io.exoquery.util
//
//import io.kotest.core.spec.style.FreeSpec
//import io.kotest.matchers.shouldBe
//
//// NOTE Don't want to move this to the `testing` project for now because this test is reliant on the Terpal plugin
//// and we don't want to be forced to add that as a dependency to the `testing` project
//class TracerSpec : FreeSpec({
//  class ToStringOutputSink : Tracer.OutputSink {
//    val info = mutableListOf<String>()
//    override fun output(str: String) {
//      info.add(str)
//    }
//
//    fun print() = info.joinToString("")
//  }
//
//  fun makeTracer() =
//    ToStringOutputSink().let { it ->
//      it to Tracer(TraceType.Standard, TraceConfig(listOf(TraceType.Standard), it), defaultIndent = 0, color = false, globalTracesEnabled = { true })
//    }
//
//  "two-level-andReturn" {
//    val (output, tracer) = makeTracer()
//    val str = tracer("level1").andReturn {
//      tracer("level2").andReturn {
//        tracer("level3").andLog()
//        "level4"
//      }
//    }
//    output.print() shouldBe run {
//      """level1
//        |    level2
//        |    level3
//        |    > "level4"
//        |> "level4"
//        |
//        """.trimMargin()
//    }
//  }
//
//
////  "two-level-andReturnIf" {
////    val (output, tracer) = makeTracer()
////    val str = tracer("level1").andReturnIf {
////      tracer("level2").andReturnIf {
////        tracer("level3").andLog()
////      }({ true })
////    }({ true })
////    println(output.value.toString())
////  }
//})
