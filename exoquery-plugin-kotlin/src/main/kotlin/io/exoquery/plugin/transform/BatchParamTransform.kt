package io.exoquery.plugin.transform

import io.exoquery.plugin.trees.IrTraversals
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue

private fun IrGetValue.isGetting(batchParam: IrValueParameter): Boolean =
  this.symbol.owner == batchParam

context(parsing: CX.Parsing)
fun IrExpression.containsBatchParam() =
  parsing.batchAlias != null && IrTraversals.collectGetValue(this).any { it.isBatchParam() }

context(parsing: CX.Parsing)
fun IrGetValue.isBatchParam() =
  parsing.batchAlias != null && this.isGetting(parsing.batchAlias)
