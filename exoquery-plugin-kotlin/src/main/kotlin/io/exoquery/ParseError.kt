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

  /**
   * In many situations we want to track the origin of the error to provide better context,
   * however many functions that need to propagate it should not have access to the original element
   * to avoid accidental misuse. Therefore we wrap it in this sealed interface to which only this file has access.
   */
  sealed interface Origin {
    companion object {
      context(scope: CX.Scope)
      fun from(element: IrElement): Origin = ErrorOriginElement(element, element.location())
    }
  }
  private data class ErrorOriginElement(val element: IrElement, val location: CompilerMessageSourceLocation) : Origin

  companion object {
    context(scope: CX.Scope)
    fun withFullMsg(msg: String, origin: Origin, file: IrFile, originalErrorTrace: Throwable? = null, showCrossFile: Boolean = false): ParseError {
      val (element, location) = when (origin) {
        is ErrorOriginElement -> origin.element to origin.location
      }
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
            if (element is IrFile) {
            """|
               |------------ Source ------------
               |File: ${element.fileEntry.name}
               |""".trimMargin().trimEnd()
            } else {
            """|
               |------------ Source ------------
               |${src}
               |------------ Raw Expression ------------
               |${rawExpression}
               |""".trimMargin().trimEnd()
            }
          }

        val rawExpressionTree =
          try {
            element.dumpSimple(scope.errorDetailsColor)
          } catch (e: Throwable) {
            try {
              element.dumpSimple(scope.errorDetailsColor)
            } catch (e: Throwable) {
              limitedStackTraceFromScope(e)
            }
          }

        val crossFileContent =
          if (showCrossFile) {
            "\n" + scope.storedXRsScope.scoped { storedXRs.printStored() }
          } else {
            ""
          }

        // If error details not enabled, walk through original error cause
        // and so long as cause is a ParseError show that
        fun traceOriginalCauseSummary(t: Throwable?): String =
          when (t) {
            null -> ""
            is ParseError ->
              t.fullMessage ?:
                traceOriginalCauseSummary(t.cause).let { summary ->
                  if (!summary.isBlank())
                    "----- Caused by: -----\n$summary"
                  else
                    ""
                }
            else -> ""
          }

        val originalErrorTrace =
          if (scope.errorDetailsEnabled) {
            originalErrorTrace?.let { "\n----------------- Original Cause: -----------------\n${it.stackTraceToString()}\n" } ?: ""
          } else {
            val originalCauseSummary = traceOriginalCauseSummary(originalErrorTrace)
            if (originalCauseSummary.isNotBlank()) {
              "\n----------------- Original Cause: -----------------\n${originalCauseSummary}"
            } else {
              ""
            }
          }

        val errorDetail =
          if (scope.errorDetailsEnabled) {
            "\n" + """------------ Raw Expression Tree ------------
            |${rawExpressionTree}
            |""".trimMargin().trimEnd()
          } else {
            ""
          }

        """[ExoQuery] Could not understand an expression or query due to an error: ${msg}${expressionPart}""" + errorDetail + originalErrorTrace + crossFileContent
      }.trimEnd()

      return ParseError(fullMsg, location, scope.errorDetailsEnabled, scope.stackCount)
    }

    context(scope: CX.Scope)
    private fun limitedStackTraceFromScope(e: Throwable) =
      e.stackTrace.takeIfPositive(scope.stackCount).map { "at $it" }.joinToString("\n") { it.toString() }
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

context(scope: CX.Scope)
fun parseErrorFromType(msg: String, expr: IrElement, originalErrorTrace: Throwable? = null): Nothing =
  throw ParseError.withFullMsg(io.exoquery.plugin.logging.Messages.TypeParseErrorMsg(msg), ParseError.Origin.from(expr), scope.currentFile, originalErrorTrace)

context(scope: CX.Scope) fun parseErrorAtCurrent(msg: String): Nothing =
  throw ParseError.withFullMsg(msg, ParseError.Origin.from(scope.currentExpr), scope.currentFile)

context(scope: CX.Scope) fun parseError(msg: String, expr: IrElement, originalErrorTrace: Throwable? = null, showCrossFile: Boolean = false): Nothing =
  throw ParseError.withFullMsg(msg, ParseError.Origin.from(expr), scope.currentFile, originalErrorTrace, showCrossFile)

context(scope: CX.Scope) fun parseError(msg: String, origin: ParseError.Origin, originalErrorTrace: Throwable? = null, showCrossFile: Boolean = false): Nothing =
  throw ParseError.withFullMsg(msg, origin, scope.currentFile, originalErrorTrace, showCrossFile)

context(scope: CX.Scope) fun parseErrorSimple(msg: String, expr: IrElement): Nothing = throw ParseError(msg, expr.location(), scope.errorDetailsEnabled, scope.stackCount)

context(scope: CX.Scope) fun parseErrorSym(expr: IrCall): Nothing =
  throw ParseError.withFullMsg("Invalid function name or symbol: ${expr.symName}", ParseError.Origin.from(expr), scope.currentFile)
