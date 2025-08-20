package io.exoquery.plugin.trees

import io.exoquery.parseError
import io.exoquery.plugin.source
import io.exoquery.plugin.transform.CX
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression

object UnlifterBasics {
  context (CX.Scope)
  fun orFail(expr: IrExpression, additionalMsg: String? = null): Nothing =
    parseError("Failed to read the compile time construct. ${additionalMsg}", expr) // TODO need a MUCH BETTER error here

  context (CX.Scope)
  fun unliftString(expr: IrExpression): String =
    (expr as? IrConst)
      ?.let {
        (it.kind as? IrConstKind.String) ?: parseError("Expected a constant string", expr)
        it.value as? String ?: parseError("Constant value was not a string", expr)
      }
      ?: orFail(
        expr,
        "Expected the expression ${expr.source()} to be a constant (i.e. compile-time) string but it was not"
      )

  context (CX.Scope)
  fun unliftInt(expr: IrExpression): Int =
    (expr as? IrConst)
      ?.let {
        (it.kind as? IrConstKind.Int) ?: parseError("Expected a constant int", expr)
        it.value as? Int ?: parseError("Constant value was not an int", expr)
      }
      ?: orFail(
        expr,
        "Expected the expression ${expr.source()} to be a constant int (e.g. the value 18) but it was not"
      )

  context (CX.Scope)
  fun unliftBoolean(expr: IrExpression): Boolean =
    (expr as? IrConst)
      ?.let {
        (it.kind as? IrConstKind.Boolean) ?: parseError("Expected a constant boolean", expr)
        it.value as? Boolean ?: parseError("Constant value was not a boolean", expr)
      }
      ?: orFail(
        expr,
        "Expected the expression ${expr.source()} to be a constant boolean (i.e. the value 'true' or 'false') but it was not"
      )

  context (CX.Scope)
  fun unliftStringIfNotNull(expr: IrExpression?) : String? =
    expr?.let { unliftString(it) }

  context (CX.Scope)
  fun unliftIntIfNotNull(expr: IrExpression?) : Int? =
    expr?.let { unliftInt(it) }

  context (CX.Scope)
  fun unliftBooleanIfNotNull(expr: IrExpression?) : Boolean? =
    expr?.let { unliftBoolean(it) }
}
