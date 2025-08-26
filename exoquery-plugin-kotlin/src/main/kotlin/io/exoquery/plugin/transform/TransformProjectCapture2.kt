package io.exoquery.plugin.transform

import io.exoquery.SqlAction
import io.exoquery.SqlBatchAction
import io.exoquery.SqlExpression
import io.exoquery.SqlQuery
import io.exoquery.plugin.isCapturedFunction
import io.exoquery.plugin.isClass
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.trees.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

class TransformProjectCapture2(val superTransformer: VisitTransformExpressions) : FalliableTransformer<IrExpression>() {

  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun matches(expression: IrExpression): Boolean =
    expression.isContainerOfXR()

  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun transform(expression: IrExpression): IrExpression? = run {
      val ownershipChain = TransformProjectCapture2.buildOwnerChain(expression)
      if (ownershipChain.root == OwnerChain.Root.Unknown)
      // If there's a Unknown in the ownership chain we can't transform/project anything and whatever is going on must be dynamic or invalid
        null
      else {
        TransformProjectCapture2.processOwnerChain(ownershipChain, superTransformer, false)
      }
  }

  // Diagnostic error for debugging uproot failure of a particular clause
  fun failOwnerUproot(sym: IrSymbol, output: IrExpression): IrExpression {
    error(
      """|----------- Could not uproot the expression: -----------
         |${sym.owner.dumpKotlinLike()}
         |--- With the following IR: ----
         |${sym.owner.dumpSimple()}""".trimMargin()
    )
    return output
  }

  companion object {
    context(CX.Scope)
    fun buildOwnerChain(expression: IrExpression) =
      OwnerChain.buildFrom(expression)

    context(CX.Scope, CX.Builder, CX.Symbology)
    fun processOwnerChain(ownershipChain: OwnerChain, superTransformer: VisitTransformExpressions, allowProcessCapturedFunction: Boolean) =
      ascendFromRootAndProjectRecurse(ownershipChain, superTransformer, allowProcessCapturedFunction)?.expr


    context(CX.Scope, CX.Builder)
    private fun IrExpression.isContainerOfXR(): Boolean =
      this.type.isClass<SqlExpression<*>>() || this.type.isClass<SqlQuery<*>>() || this.type.isClass<SqlAction<*, *>>() || this.type.isClass<SqlBatchAction<*, *, *>>()

    sealed interface ExprType {
      data object Expr : ExprType
      data object Query : ExprType
      data object Action : ExprType
      data object ActionBatch : ExprType
    }

    context(CX.Scope, CX.Builder, CX.Symbology)
    private fun IrExpression.exprTypeOf(): ExprType? =
      when {
        this.type.isClass<SqlExpression<*>>() -> ExprType.Expr
        this.type.isClass<SqlQuery<*>>() -> ExprType.Query
        this.type.isClass<SqlAction<*, *>>() -> ExprType.Action
        this.type.isClass<SqlBatchAction<*, *, *>>() -> ExprType.ActionBatch // Don't have the uprootable yet
        //else -> parseError("The expression is not a SqlExpression or SqlQuery, (its type is ${this.type.dumpKotlinLike()} which cannot be projected)", this)
        else -> null
      }

    context(CX.Scope, CX.Builder, CX.Symbology)
    fun ascendFromRootAndProjectRecurse(node: OwnerChain, superTransformer: VisitTransformExpressions, allowProcessCapturedFunction: Boolean): OwnerChain.Root.Uprootable? {
      fun ascendFromRootAndProject(node: OwnerChain) = ascendFromRootAndProjectRecurse(node, superTransformer, allowProcessCapturedFunction)

      // if we're not not the root recurse further up the chain
      return when {
        // Do not process captured functions here
        node is OwnerChain.SourcedFunction && node.origin.isCapturedFunction() && !allowProcessCapturedFunction -> null

        // ================ If we're at the root (i.e. the element is of type OwnerChain.Root) we need to determine what to do based on the type of the root element ================
        // 0. If it's null a previous operation has failed, return null and we're done
        // 1. If we're a Uprootable then just go up one level, the level above will will handle capture-projection
        // 2. If we're not an uprootable try doing the superTransform.visitCall on it. If it becomes an uprootable then return it, the level above will handle capture-projection
        // Note be sure to NEVER return a null other than from anything but a root since this sequence of operations modifies intermeidate expressions
        // and returning `null` effectively tells the upstream "nothing has happened".
        node is OwnerChain.Root -> {
          when (node) {
            is OwnerChain.Root.Unknown -> null

            is OwnerChain.Root.UprootableAction -> node
            is OwnerChain.Root.UprootableActionBatch -> node
            is OwnerChain.Root.UprootableExpr -> node
            is OwnerChain.Root.UprootableQuery -> node

            // If the expression inside is a virgin transform, we try to transform it i.e. give it a chance to become an uprootable
            // if the transform doesn't match the expression we return null and the entire chain passes it down
            is OwnerChain.Root.VirginQuery -> {
              val transformer = TransformCapturedQuery(superTransformer, "<Project>")
              if (transformer.matches(node.call)) transformer.transform(node.call).withUprootableQueryOrNull()?.let { (expr, uproot) -> OwnerChain.Root.UprootableQuery(expr, uproot) } else null
            }
            is OwnerChain.Root.VirginExpr -> {
              val transformer = TransformCapturedExpression(superTransformer)
              if (transformer.matches(node.call)) transformer.transform(node.call).withUprootableExpressionOrNull()?.let { (expr, uproot) -> OwnerChain.Root.UprootableExpr(expr, uproot) } else null
            }
            is OwnerChain.Root.VirginAction -> {
              val transformer = TransformCapturedAction(superTransformer)
              if (transformer.matches(node.call)) transformer.transform(node.call).withUprootableActionOrNull()?.let { (expr, uproot) -> OwnerChain.Root.UprootableAction(expr, uproot) } else null
            }
            is OwnerChain.Root.VirginActionBatch -> {
              val transformer = TransformCapturedBatchAction(superTransformer)
              if (transformer.matches(node.call)) transformer.transform(node.call).withUprootableBatchActionOrNull()?.let { (expr, uproot) -> OwnerChain.Root.UprootableActionBatch(expr, uproot) } else null
            }
          }
        }

        // ================ If we're not a Root that means some previous root up the chain succeeded (either doing #1 or #2 above) do we capture-projection and go up the chain ================
        // Instructions: say we've got a case of:
        // fun foo() = capture { Table<Person>() }
        // fun bar() = capture { foo().filter { it.name == "Alice" } }
        // `fun foo()` will be the node.origin and will be the uprootableParent when we come back from it
        // `node` will be the `foo()` node.call` IrCall from bar that we need to project into an SqlQuery(..., params=params + foo().params) call
        // That means we need to:
        // 1. Replace the `fun foo()` body with the parent-uprootable (i.e the SqlQuery/SqlExpression,... container)
        // 2. Replace the `foo()` invocation with `SqlQuery(..., params=params + foo().params)` projection (i.e. replant the uprootable parent expression)
        // 3. Return this replaced invocation to the next call up
        //
        // Note that ironically, as we ascend up the chain, we make every step of the chain into an root (all uprootables are roots) which is exactly as intended.
        node is OwnerChain.SourcedFunction -> {
          val uprootableParent = ascendFromRootAndProject(node.parent) ?: return null
          // 1. Replace the `fun foo()` body with the parent SqlQuery/SqlExpression,... container
          // It could either have already been an uprootable (so it's a no-op) or it could be a virgin that we made into an uprootable
          val nodeOriginBefore = node.origin.dumpKotlinLike()

          node.origin.replaceSingleReturnBodyWith(uprootableParent.expr)

          val nodeOriginAfter = node.origin.dumpKotlinLike()

          // 2. Replace the `foo()` invocation with `SqlQuery(..., params=params + foo().params)` projection (i.e. replant the uprootable parent expression)
          // (also does) 3. Return this replaced invocation to the next call up
          uprootableParent.replantUprootableWith(node.call)
        }
        // Follow the same instructions as above, but for a field
        node is OwnerChain.SourcedField -> {
          val uprootableParent = ascendFromRootAndProject(node.parent) ?: return null
          node.origin.replaceInitializerBodyWith(uprootableParent.expr)
          uprootableParent.replantUprootableWith(node.call)
        }
        // Follow the same instructions as above, but for a variable
        node is OwnerChain.SourcedVariable -> {
          val uprootableParent = ascendFromRootAndProject(node.parent) ?: return null
          node.origin.replaceInitializerBodyWith(uprootableParent.expr)
          // @CompiledQuery annotation so that if the variable is access from another file etc... it can be used
          node.origin.annotations = node.origin.annotations + makeLifter().makeCompiledQueryAnnotation(uprootableParent.uprootable.packedXR)
          uprootableParent.replantUprootableWith(node.call)
        }
        else -> null
      }
    }


  }
}
