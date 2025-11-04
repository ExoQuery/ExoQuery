package io.exoquery.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.parseError
import io.exoquery.plugin.trees.ExtractorsDomain
import io.exoquery.plugin.trees.Parser
import io.exoquery.plugin.trees.SqlExpressionExpr
import io.exoquery.xr.XR
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression


class TransformCapturedExpression(val superTransformer: VisitTransformExpressions) : Transformer<IrCall>() {
  context(CX.Scope, CX.Builder)
  override fun matches(expression: IrCall): Boolean =
    ExtractorsDomain.Call.CaptureExpression[Is()].matchesAny(expression)

  // parent symbols are collected in the parent context
  context(CX.Scope, CX.Builder)
  override fun transform(expression: IrCall): IrExpression {
    val (xrExpr, dynamics) = parseSqlExpression(expression, superTransformer)
    val paramsExprModel = dynamics.makeParams()
    //val make = makeClassFromString("io.exoquery.SqlExpression", listOf(strExpr, paramsListExpr))
    //val makeCasted = builder.irImplicitCast(make, expression.type)
    val newSqlExpression =
      if (dynamics.noRuntimes()) {
        SqlExpressionExpr.Uprootable.plantNewUprootable(xrExpr, paramsExprModel)
      } else {
        SqlExpressionExpr.Uprootable.plantNewPluckable(xrExpr, dynamics.makeRuntimes(), paramsExprModel)
      }

    //logger.warn("=============== Modified value to: ${capturedAnnot.regularArgs[0]?.dumpKotlinLike()}\n======= Whole Type is now:\n${makeCasted.type.dumpKotlinLike()}")
    //logger.error("========== Output: ==========\n${newSqlExpression.dumpKotlinLike()}")
    return newSqlExpression
  }

  companion object {
    context(CX.Scope, CX.Builder)
    fun parseSqlExpression(expression: IrCall, superTransformer: VisitTransformExpressions) = run {
      val bodyRaw =
        on(expression).match(
          // printExpr(.. { stuff }: IrFunctionExpression  ..): FunctionCall
          case(ExtractorsDomain.Call.CaptureExpression.LambdaBody[Is()]).then { body ->
            body
          }
        ) ?: parseError("The expression has an invalid structure", expression)

      // Transform the contents of `sql { ... }` this is important for several reasons,
      // most notable any kind of variables used inside that need to be inlined e.g:
      // val x = sql { 123 }
      // val y = sql { x.use + 1 } // <- this is what we are transforming
      // Then the `val y` needs to first be transformed into:
      // val y = sql { SqlExpression(XR.Int(123), ...).use + 1 } which will be done by TransformProjectCapture
      // which is called by the superTransformer.visitBlockBody
      val body = superTransformer.visitBlockBody(bodyRaw) as IrBlockBody
      val (xr, dynamics) = Parser.scoped { Parser.parseFunctionBlockBody(body) }

      val xrExpr = xr as? XR.Expression ?: parseError("Could not parse to expression", expression)
      xrExpr to dynamics
    }
  }
}
