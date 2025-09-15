package io.exoquery.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.parseError
import io.exoquery.plugin.loc
import io.exoquery.plugin.logging.Messages
import io.exoquery.plugin.trees.ExtractorsDomain
import io.exoquery.plugin.trees.Parser
import io.exoquery.plugin.trees.SqlActionExpr
import io.exoquery.plugin.trees.SqlBatchActionExpr
import io.exoquery.xr.XR
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression

class TransformCapturedAction(val superTransformer: VisitTransformExpressions) : Transformer<IrCall>() {
  context(CX.Scope, CX.Builder)
  override fun matches(expression: IrCall): Boolean =
    ExtractorsDomain.Call.CaptureAction[Is()].matchesAny(expression)

  context(CX.Scope, CX.Builder)
  override fun transform(expression: IrCall): IrExpression {
    val (xr, dynamics) = parseCapturedAction(expression, superTransformer)

    val paramsExprModel = dynamics.makeParams()
    val newSqlAction =
      if (dynamics.noRuntimes()) {
        SqlActionExpr.Uprootable.plantNewUprootable(xr, paramsExprModel, expression.type)
      } else {
        SqlActionExpr.Uprootable.plantNewPluckable(xr, dynamics.makeRuntimes(), paramsExprModel, expression.type)
      }

    //logger.error("========== Output: ==========\n${newSqlAction.dumpKotlinLike()}")
    return newSqlAction
  }

  companion object {
    context(CX.Scope, CX.Builder)
    fun parseCapturedAction(expression: IrCall, superTransformer: VisitTransformExpressions) = run {
      val bodyExpr =
        on(expression).match(
          // printExpr(.. { stuff }: IrFunctionExpression  ..): FunctionCall
          case(ExtractorsDomain.Call.CaptureAction.LambdaOutput[Is()]).then { expr ->
            expr
          }
        )
          ?: parseError(Messages.InvalidSqlActionFunctionBody(), expression)

      val body = superTransformer.recurse(bodyExpr)
      Parser.scoped { Parser.parseAction(body) }
    }
  }
}

class TransformCapturedBatchAction(val superTransformer: VisitTransformExpressions) : Transformer<IrCall>() {
  context(CX.Scope, CX.Builder)
  override fun matches(expression: IrCall): Boolean =
    ExtractorsDomain.Call.CaptureBatchAction[Is.Companion()].matchesAny(expression)

  context(CX.Scope, CX.Builder)
  override fun transform(expression: IrCall): IrExpression {
    val (xr, dynamics, batchCollection) = parseCapturedBatchAction(expression, superTransformer)

    val paramsExprModel = dynamics.makeParams()
    val newSqlAction =
      if (dynamics.noRuntimes()) {
        SqlBatchActionExpr.Uprootable.plantNewUprootable(xr, batchCollection, paramsExprModel)
      } else {
        SqlBatchActionExpr.Uprootable.plantNewPluckable(xr, batchCollection, dynamics.makeRuntimes(), paramsExprModel)
      }

    //logger.error("========== Output: ==========\n${newSqlAction.dumpKotlinLike()}")
    return newSqlAction
  }

  companion object {
    context(CX.Scope, CX.Builder)
    fun parseCapturedBatchAction(expression: IrCall, superTransformer: VisitTransformExpressions) = run {
      val (batchData, bodyExpr) =
        on(expression).match(
          // printExpr(.. { stuff }: IrFunctionExpression  ..): FunctionCall
          case(ExtractorsDomain.Call.CaptureBatchAction.LambdaOutput[Is(), Is()]).then { batchParam, expr ->
            batchParam to expr
          }
        )
          ?: parseError(Messages.InvalidSqlActionFunctionBody(), expression)

      val (batchParam, batchCollection) = batchData

      val body = superTransformer.recurse(bodyExpr)
      val (actionXR, dynamics) = Parser.scoped(CX.Parsing(batchAlias = batchParam)) { Parser.parseAction(body) }
      val batchParamAlias = Parser.scoped { Parser.parseValueParamter(batchParam) }
      val xr = XR.Batching(batchParamAlias, actionXR, body.loc)
      Triple(xr, dynamics, batchCollection)
    }
  }
}
