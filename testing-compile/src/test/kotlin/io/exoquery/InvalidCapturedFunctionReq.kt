package io.exoquery

class InvalidCapturedFunctionReq : MessageSpecDynamic(InvalidCapturedFunctionReqGoldenDynamic, Mode.ExoGoldenTest(), {
  "error if captured function is not a SqlQuery<T> or SqlExpression<T>" {
    shouldBeGoldenError(
      """
      import io.exoquery.*
      import io.exoquery.annotation.SqlFragment
      data class MyPerson(val name: String, val age: Int)
      @SqlFragment
      fun externalFunction(name: String) = name + "_suffix"
      fun run() {
        val q = sql { Table<MyPerson>().filter { p -> externalFunction(p.name) == "Joe" } }
        println(q.buildFor.Postgres().value)
      }
      """.trimIndent()
    )
  }

  /**
   * A Captured Function is traversed at least twice, once during the VisitTransformExpressions phase and potentially once again
   * during the bubble-up traversal of the TransformAnnotatedFunction phase. Both of these call-chains eventually end up detecting
   * that the Captured Function has an invalid return type and report errors, however if there is no stack-trace reported in the error
   * the Kotlin compiler will de-duplicate the error messages (since they have the same text and location) and only show one of them.
   * This is the correct and desired behavior.
   *
   * In situations where we want to see the full stack trace however, the two errors will no longer be identical. In that situation
   * will will have two different stack trace appear for example:
   * ```
   * // This:
   * [ExoQuery] Could not understand an expression or query due to an error: The SqlFragment had the wrong kind of return type
   * ...
   * ----------------- Stack Trace: -----------------
   * at io.exoquery.ParseErrorCompanion.withFullMsg(ParseError.kt:84)
   * at io.exoquery.ParseErrorKt.parseError(ParseError.kt:129)
   * at io.exoquery.ParseErrorKt.parseErrordefault(ParseError.kt:128)
   * at io.exoquery.plugin.transform.TransformAnnotatedFunction.transform(TransformAnnotatedFunction.kt:125)
   * at io.exoquery.plugin.transform.TransformScaffoldAnnotatedFunctionCall.transform(TransformScaffoldAnnotatedFunctionCall.kt:75)
   * at io.exoquery.plugin.transform.VisitTransformExpressions.visitCalllambda1(VisitTransformExpressions.kt:256)
   * at io.exoquery.plugin.transform.VisitTransformExpressionsScopedRunner.runlambda0runBlock(VisitTransformExpressions.kt:174)
   * at io.exoquery.plugin.transform.VisitTransformExpressionsScopedRunner.run(VisitTransformExpressions.kt:177)
   * at io.exoquery.plugin.transform.VisitTransformExpressions.visitCall(VisitTransformExpressions.kt:254)
   * at io.exoquery.plugin.transform.VisitTransformExpressions.visitCall(VisitTransformExpressions.kt:38)
   * ... (truncated)
   * // ...and this:
   * [ExoQuery] Could not understand an expression or query due to an error: The SqlFragment had the wrong kind of return type
   * ...
   * ----------------- Stack Trace: -----------------
   * at io.exoquery.ParseErrorCompanion.withFullMsg(ParseError.kt:84)
   * at io.exoquery.ParseErrorKt.parseError(ParseError.kt:129)
   * at io.exoquery.ParseErrorKt.parseErrordefault(ParseError.kt:128)
   * at io.exoquery.plugin.trees.ParseExpression.parse(ParseExpression.kt:567)
   * at io.exoquery.plugin.trees.ParseExpressionparseinlinedthenThis19.invoke(ThenPattern1.kt:148)
   * at io.decomat.StageCase.eval(Matching.kt:68)
   * at io.decomat.DoMatch.match(Matching.kt:29)
   * at io.exoquery.plugin.trees.ParseExpression.parse(ParseExpression.kt:115)
   * at io.exoquery.plugin.trees.ParseExpressionparseFunctionBlockBodyinlinedthen1.invoke(ThenPattern1.kt:147)
   * at io.decomat.StageCase.eval(Matching.kt:68)
   * ... (truncated)
   * ```
   * However, since stack traces change so often (i.e. whenever a source file is modified even in the slightest way) it is
   * identical anymore therefore it is not practical to include them in golden tests so there is a mechanism in MessageFileKotlinMaker
   * that reduces an error stack to a single message [Excluding X lines] where X is the number of lines in the original stack trace.
   * This allows us to still verify that stack traces are being generated while not caring about their exact content.
   *
   * Since there are two different stack traces in this test case but MessageFileKotlinMaker reduces both of them to the same
   * [Excluding X lines] message the final output of both errors will look the same but it represents two different error stacks.
   */
  "error if captured function is not a SqlQuery<T> or SqlExpression<T> - with details" {
    shouldBeGoldenError(
      """
      @file:io.exoquery.annotation.ErrorDetailsEnabled(false)
      import io.exoquery.*
      import io.exoquery.annotation.SqlFragment
      data class MyPerson(val name: String, val age: Int)
      @SqlFragment
      fun externalFunction(name: String) = name + "_suffix"
      fun run() {
        val q = sql { Table<MyPerson>().filter { p -> externalFunction(p.name) == "Joe" } }
        println(q.buildFor.Postgres().value)
      }
      """.trimIndent()
    )
  }
})
