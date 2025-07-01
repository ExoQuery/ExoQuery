package io.exoquery.plugin.transform

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrExpression

abstract class TransformerWithQueryAccum<Expr : IrExpression> {
  context(CX.Scope, CX.Builder, CX.Symbology)
  abstract fun matches(expression: Expr): Boolean

  context(CX.Scope, CX.Builder, CX.Symbology, CX.QueryAccum)
  abstract fun transform(expression: Expr): IrExpression
}

abstract class Transformer<Expr : IrExpression> {
  context(CX.Scope, CX.Builder, CX.Symbology)
  abstract fun matches(expression: Expr): Boolean

  context(CX.Scope, CX.Builder, CX.Symbology)
  abstract fun transform(expression: Expr): IrExpression
}

abstract class FalliableTransformer<Expr : IrExpression> {
  context(CX.Scope, CX.Builder, CX.Symbology)
  abstract fun matches(expression: Expr): Boolean

  context(CX.Scope, CX.Builder, CX.Symbology)
  abstract fun transform(expression: Expr): IrExpression?
}

abstract class FalliableElementTransformer<Expr : IrElement> {
  // properties that are dependent on ctx need lazy initialization
  context(CX.Scope, CX.Builder, CX.Symbology)
  abstract fun matches(expression: Expr): Boolean

  context(CX.Scope, CX.Builder, CX.Symbology)
  abstract fun transform(expression: Expr): Expr?
}

abstract class ElementTransformer<Expr : IrElement> {
  context(CX.Scope, CX.Builder, CX.Symbology)
  abstract fun matches(expression: Expr): Boolean

  context(CX.Scope, CX.Builder, CX.Symbology)
  abstract fun transform(expression: Expr): Expr
}
