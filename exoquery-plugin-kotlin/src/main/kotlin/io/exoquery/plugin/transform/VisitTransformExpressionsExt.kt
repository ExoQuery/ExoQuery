package io.exoquery.plugin.transform

import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression

context(accum: CX.QueryAccum) fun makeVisitorContextWithAccum() = VisitorContext(accum.accum)

fun makeVisitorContext() = VisitorContext(FileQueryAccum.empty())

fun VisitTransformExpressions.visitExpression(expression: IrExpression) =
  visitExpression(expression, makeVisitorContext())

fun VisitTransformExpressions.visitBlockBody(body: IrBlockBody) =
  visitBlockBody(body, makeVisitorContext())

fun VisitTransformExpressions.visitCall(expression: IrCall) =
  visitCall(expression, makeVisitorContext())
