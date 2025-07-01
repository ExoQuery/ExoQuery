package io.exoquery.plugin.transform

import io.exoquery.plugin.transform.CX
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression

context(CX.Symbology, CX.QueryAccum) fun makeVisitorContextWithAccum() = VisitorContext(symbolSet, accum)

context(CX.Symbology) fun makeVisitorContext() = VisitorContext(symbolSet, FileQueryAccum.empty())

context (CX.Symbology) fun VisitTransformExpressions.visitExpression(expression: IrExpression) =
  visitExpression(expression, makeVisitorContext())

context (CX.Symbology) fun VisitTransformExpressions.visitBlockBody(body: IrBlockBody) =
  visitBlockBody(body, makeVisitorContext())

context (CX.Symbology) fun VisitTransformExpressions.visitCall(expression: IrCall) =
  visitCall(expression, makeVisitorContext())
