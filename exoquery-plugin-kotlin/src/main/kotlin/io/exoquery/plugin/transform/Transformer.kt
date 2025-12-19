package io.exoquery.plugin.transform

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrExpression

abstract class TransformerWithQueryAccum<Expr : IrExpression> {
  context(scope: CX.Scope, builder: CX.Builder)
  abstract fun matches(expression: Expr): Boolean

  context(scope: CX.Scope, builder: CX.Builder, accum: CX.QueryAccum)
  abstract fun transform(expression: Expr): IrExpression
}

abstract class Transformer<Expr : IrExpression> {
  context(scope: CX.Scope, builder: CX.Builder)
  abstract fun matches(expression: Expr): Boolean

  context(scope: CX.Scope, builder: CX.Builder)
  abstract fun transform(expression: Expr): IrExpression
}

abstract class FalliableTransformer<Expr : IrExpression> {
  context(scope: CX.Scope, builder: CX.Builder)
  abstract fun matches(expression: Expr): Boolean

  context(scope: CX.Scope, builder: CX.Builder)
  abstract fun transform(expression: Expr): IrExpression?
}

abstract class FalliableElementTransformer<Expr : IrElement> {
  // properties that are dependent on ctx need lazy initialization
  context(scope: CX.Scope, builder: CX.Builder)
  abstract fun matches(expression: Expr): Boolean

  context(scope: CX.Scope, builder: CX.Builder)
  abstract fun transform(expression: Expr): Expr?
}

abstract class ElementTransformer<Expr : IrElement> {
  context(scope: CX.Scope, builder: CX.Builder)
  abstract fun matches(expression: Expr): Boolean

  context(scope: CX.Scope, builder: CX.Builder)
  abstract fun transform(expression: Expr): Expr
}
