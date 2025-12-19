package io.exoquery.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.parseError
import io.exoquery.plugin.sourceOrDump
import io.exoquery.plugin.trees.ExtractorsDomain
import io.exoquery.plugin.trees.Parser
import io.exoquery.plugin.trees.SqlQueryExpr
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dumpKotlinLike


class TransformCapturedQuery(val superTransformer: VisitTransformExpressions, val calledFrom: String) : Transformer<IrCall>() {

  context(scope: CX.Scope, builder: CX.Builder)
  override fun matches(expression: IrCall): Boolean =
    ExtractorsDomain.Call.CaptureQuery[Is()].matchesAny(expression)

  // parent symbols are collected in the parent context
  context(scope: CX.Scope, builder: CX.Builder)
  override fun transform(expression: IrCall): IrExpression {
    val (xr, dynamics) = parseCapturedQuery(expression, superTransformer)

    val paramsExprModel = dynamics.makeParams()
    val newSqlQuery =
      if (dynamics.noRuntimes()) {
        SqlQueryExpr.Uprootable.plantNewUprootable(xr, paramsExprModel)
      } else {
        //parseError("----------------------- Pluckable Query Found -----------------------\n${expression.sourceOrDump()}\n---------------- Dynamics Were --------------------${dynamics.getAllRuntimesCollect().withIndex().map { (i, it) -> "$i) ${it.sourceOrDump()}" }.joinToString("\n")}")
        SqlQueryExpr.Uprootable.plantNewPluckable(xr, dynamics.makeRuntimes(), paramsExprModel)
      }

    //    if (currentFile.fileEntry.name.toString().contains("CapFun")) {
    //      logger.error(
    //        """|--------------------------- TRANSFORM CAPTURE (${calledFrom}) From ---------------------------
    //           |${expression.dumpKotlinLike()}
    //           |------------------------- TRANSFORM CAPTURE (${calledFrom}) To -------------------------
    //           |${newSqlQuery.prepareForPrinting()?.dumpKotlinLike()}
    //        """.trimMargin()
    //      )
    //    }

    return newSqlQuery
  }

  companion object {
    context(scope: CX.Scope, builder: CX.Builder)
    fun parseCapturedQuery(expression: IrCall, superTransformer: VisitTransformExpressions) = run {
      val bodyExpr =
        on(expression).match(
          // printExpr(.. { stuff }: IrFunctionExpression  ..): FunctionCall
          case(ExtractorsDomain.Call.CaptureQuery.LambdaBody[Is()]).then { expr ->
            expr
          }
        )
          ?: parseError("The qeury expression has an invalid structure", expression)

      // Transform the contents of `sql { ... }` this is important for several reasons,
      // most notable any kind of variables used inside that need to be inlined e.g:
      // val x = sql { 123 }
      // val y = sql { x.use + 1 } // <- this is what we are transforming
      // Then the `val y` needs to first be transformed into:
      // val y = sql { SqlExpression(XR.Int(123), ...).use + 1 } which will be done by TransformProjectCapture
      // which is called by the superTransformer.visitBlockBody
      val body = superTransformer.recurse(bodyExpr)
      Parser.scoped { Parser.parseQueryFromBlock(body) }
    }
  }
}
