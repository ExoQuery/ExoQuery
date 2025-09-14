package io.exoquery

import io.exoquery.plugin.location
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.source
import io.exoquery.plugin.symName
import io.exoquery.plugin.transform.CX
import io.exoquery.plugin.transform.prepareForPrintingAdHoc
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

class LiftingError(val msg: String) : Exception(msg)

fun liftingError(msg: String): Nothing = throw LiftingError(msg)

class ParseError(val msg: String, val location: CompilerMessageSourceLocation?) : Exception(msg) {
  companion object {
    context(CX.Scope)
    fun withFullMsg(msg: String, element: IrElement, file: IrFile, location: CompilerMessageSourceLocation, originalErrorTrace: Throwable? = null, showCrossFile: Boolean = false): ParseError {
      val fullMsg: String = run {
        val expressionPart =
          element.source()?.let { src ->
            """|
               |------------ Source ------------
               |${src}""".trimMargin()
          }

        val printingElement = element //.prepareForPrinting()
        val rawExpression =
          try {
            printingElement.dumpKotlinLike().prepareForPrintingAdHoc()
          } catch (e: Throwable) {
            try {
              element.dumpKotlinLike()
            } catch (e: Throwable) {
              e.stackTraceToString()
            }
          }
        val rawExpressionTree =
          try {
            printingElement.dumpSimple()
          } catch (e: Throwable) {
            try {
              element.dumpSimple()
            } catch (e: Throwable) {
              e.stackTraceToString()
            }
          }

        val crossFileContent =
          if (showCrossFile) {
            "\n" + storedXRsScope.scoped { storedXRs.printStored() }
          } else {
            ""
          }

        val originalErrorTrace =
          originalErrorTrace?.let { "\n----------------- Original Cause: -----------------\n${it.stackTraceToString()}\n" } ?: ""

        """|[ExoQuery] Could not understand an expression or query due to an error: ${msg}.${expressionPart}
           |------------ Raw Expression ------------
           |${rawExpression}
           |------------ Raw Expression Tree ------------
           |${rawExpressionTree}
           |""".trimMargin() + originalErrorTrace + crossFileContent
      }
      return ParseError(fullMsg, location)
    }
  }
}

private fun tryDump(dump: () -> String): String = try {
  dump()
} catch (e: Exception) {
  "Could not dump the expression due to an error: ${e.message}\n${e.stackTraceToString()}"
}

fun parseError(msg: String, location: CompilerMessageSourceLocation? = null): Nothing = throw ParseError(msg, location)

fun parseErrorFromType(msg: String, location: CompilerMessageSourceLocation): Nothing =
  throw ParseError(io.exoquery.plugin.logging.Messages.TypeParseErrorMsg(msg), location)

fun parseErrorFromType(msg: String, e: Throwable, location: CompilerMessageSourceLocation): Nothing =
  throw ParseError(io.exoquery.plugin.logging.Messages.TypeParseErrorMsg(msg + "\n----------------- Cause: -----------------\n" + e.stackTraceToString()), location)

context(CX.Scope)
fun parseErrorFromType(msg: String, expr: IrElement): Nothing = throw throw ParseError.withFullMsg(io.exoquery.plugin.logging.Messages.TypeParseErrorMsg(msg), expr, currentFile, expr.location())

context(CX.Scope) fun parseError(msg: String, expr: IrElement, originalErrorTrace: Throwable? = null, showCrossFile: Boolean = false): Nothing =
  throw ParseError.withFullMsg(msg, expr, currentFile, expr.location(), originalErrorTrace, showCrossFile)

context(CX.Scope) fun parseErrorSimple(msg: String, expr: IrElement): Nothing = throw ParseError(msg, expr.location())

context(CX.Scope) fun parseErrorSym(expr: IrCall): Nothing =
  throw ParseError.withFullMsg("Invalid function name or symbol: ${expr.symName}", expr, currentFile, expr.location())
