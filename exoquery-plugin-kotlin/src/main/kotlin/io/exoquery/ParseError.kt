package io.exoquery

import io.exoquery.plugin.location
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.source
import io.exoquery.plugin.symName
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.transform.prepareForPrintingAdHoc
import io.exoquery.printing.StaticStrings
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

class LiftingError(val msg: String) : Exception(msg)

fun liftingError(msg: String): Nothing = throw LiftingError(msg)

class ParseError(val fullMessage: String, val location: CompilerMessageSourceLocation?, val addStackToPrint: Boolean, val stackCount: Int) : Exception(fullMessage) {
  companion object {
    context(CX.Scope)
    fun withFullMsg(msg: String, element: IrElement, file: IrFile, location: CompilerMessageSourceLocation, originalErrorTrace: Throwable? = null, showCrossFile: Boolean = false): ParseError {
      val fullMsg: String = run {
        val rawExpression =
          try {
            element.dumpKotlinLike().prepareForPrintingAdHoc()
          } catch (e: Throwable) {
            try {
              "${element.dumpKotlinLike()}\n----------------- Could not prepare for printing due to error: -----------------\n${e.stackTraceToString()}"
            } catch (e: Throwable) {
              limitedStackTraceFromScope(e)
            }
          }

        val expressionPart =
          element.source()?.let { src ->
            """|
               |------------ Source ------------
               |${src}
               |------------ Raw Expression ------------
               |${rawExpression}
               |""".trimMargin().trimEnd()
          }

        val rawExpressionTree =
          try {
            element.dumpSimple(errorDetailsColor)
          } catch (e: Throwable) {
            try {
              element.dumpSimple(errorDetailsColor)
            } catch (e: Throwable) {
              limitedStackTraceFromScope(e)
            }
          }

        val crossFileContent =
          if (showCrossFile) {
            "\n" + storedXRsScope.scoped { storedXRs.printStored() }
          } else {
            ""
          }

        val originalErrorTrace =
          if (errorDetailsEnabled) {
            originalErrorTrace?.let { "\n----------------- Original Cause: -----------------\n${it.stackTraceToString()}\n" } ?: ""
          } else {
            ""
          }

        val errorDetail =
          if (errorDetailsEnabled) {
            "\n" + """------------ Raw Expression Tree ------------
            |${rawExpressionTree}
            |""".trimMargin().trimEnd()
          } else {
            ""
          }

        """[ExoQuery] Could not understand an expression or query due to an error: ${msg}.${expressionPart}""" + errorDetail + originalErrorTrace + crossFileContent
      }.trimEnd()

      return ParseError(fullMsg, location, errorDetailsEnabled, stackCount)
    }

    context(CX.Scope)
    private fun limitedStackTraceFromScope(e: Throwable) =
      e.stackTrace.takeIfPositive(stackCount).map { "at $it" }.joinToString("\n") { it.toString() }
  }

  private fun limitedStackTrace(e: Exception) =
    e.stackTrace.takeIfPositive(stackCount).map { "at $it" }.joinToString("\n") { it.toString() }

  fun fullMessageWithStackTrace(): String =
    if (addStackToPrint) {
      """${fullMessage}
      |${StaticStrings.StackTraceHeader}
      |${limitedStackTrace(this) + if (this.stackTrace.size > stackCount) "\n${StaticStrings.TruncationLine}" else "" }
      |""".trimMargin().trimEnd()
    } else {
      fullMessage
    }
}

private fun <T> Array<T>.takeIfPositive(count: Int): List<T> =
  if (count < 0) {
    this.toList()
  } else {
    this.take(count)
  }

private fun tryDump(dump: () -> String): String = try {
  dump()
} catch (e: Exception) {
  "Could not dump the expression due to an error: ${e.message}\n${e.stackTraceToString()}"
}

//fun parseError(msg: String, location: CompilerMessageSourceLocation? = null): Nothing = throw ParseError(msg, location)

context(CX.Scope)
fun parseErrorFromType(msg: String, expr: IrElement, originalErrorTrace: Throwable? = null): Nothing =
  throw ParseError.withFullMsg(io.exoquery.plugin.logging.Messages.TypeParseErrorMsg(msg), expr, currentFile, expr.location(), originalErrorTrace)

context(CX.Scope) fun parseErrorAtCurrent(msg: String): Nothing =
  throw ParseError.withFullMsg(msg, currentExpr, currentFile, currentExpr.location())

context(CX.Scope) fun parseError(msg: String, expr: IrElement, originalErrorTrace: Throwable? = null, showCrossFile: Boolean = false): Nothing =
  throw ParseError.withFullMsg(msg, expr, currentFile, expr.location(), originalErrorTrace, showCrossFile)

context(CX.Scope) fun parseErrorSimple(msg: String, expr: IrElement): Nothing = throw ParseError(msg, expr.location(), errorDetailsEnabled, stackCount)

context(CX.Scope) fun parseErrorSym(expr: IrCall): Nothing =
  throw ParseError.withFullMsg("Invalid function name or symbol: ${expr.symName}", expr, currentFile, expr.location())
