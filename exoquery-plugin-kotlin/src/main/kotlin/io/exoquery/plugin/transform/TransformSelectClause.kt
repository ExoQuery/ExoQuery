package io.exoquery.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.match
import io.exoquery.parseError
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.regularArgs
import io.exoquery.plugin.trees.ExtractorsDomain
import io.exoquery.plugin.trees.Parser
import io.exoquery.plugin.trees.SqlQueryExpr
import io.exoquery.xr.XR
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dumpKotlinLike


class TransformSelectClause(val superTransformer: VisitTransformExpressions) : Transformer<IrCall>() {

  context(scope: CX.Scope, builder: CX.Builder)
  override fun matches(expression: IrCall): Boolean =
    ExtractorsDomain.Call.CaptureSelect[Is()].matchesAny(expression)

  // parent symbols are collected in the parent context
  context(scope: CX.Scope, builder: CX.Builder)
  override fun transform(selectExpressionRaw: IrCall): IrExpression {
    val (xr, dynamics) = parseSelectClause(selectExpressionRaw, superTransformer)
    val paramsExprModel = dynamics.makeParams()
    val newSqlQuery =
      if (dynamics.noRuntimes()) {
        SqlQueryExpr.Uprootable.plantNewUprootable(xr, paramsExprModel)
      } else {
        SqlQueryExpr.Uprootable.plantNewPluckable(xr, dynamics.makeRuntimes(), paramsExprModel)
      }

    //logger.warn("=============== Modified value to: ${capturedAnnot.regularArgs[0]?.dumpKotlinLike()}\n======= Whole Type is now:\n${makeCasted.type.dumpKotlinLike()}")
    //logger.warn("========== Query Output: ==========\n${newSqlQuery.dumpKotlinLike()}")
    return newSqlQuery
  }

  companion object {
    context(scope: CX.Scope, builder: CX.Builder)
    fun parseSelectClause(selectExpressionRaw: IrCall, superTransformer: VisitTransformExpressions) = run {
      // since there could be SqlQuery clauses inside we need to recurisvely transform the stuff inside the Select-Clause first
      // therefore we need to call the super-transform on the select lambda
      val selectLambda =
        selectExpressionRaw.match(
          case(ExtractorsDomain.Call.CaptureSelect[Is()]).then {
            val selectLambdaRaw =
              it.regularArgs.firstOrNull() ?: parseError("The select clause must have a lambda as the first argument but it was null or empty (${it.regularArgs.firstOrNull()})", selectExpressionRaw)
            superTransformer.recurse(selectLambdaRaw)
          }
          // TODO use Messages.kt, use better example
        )
          ?: parseError("The clause was not a property select-expression (was the owner annotated correctly?)", selectExpressionRaw)

      val (selectClause, dynamics) = Parser.scoped { Parser.parseSelectClauseLambda(selectLambda) }
      // Store the selectClause inside a XR.CustomQueryRef instance so that we can invoke the XR serialization and "plant" it into the IR
      // This way we can directly expose this SelectClause for much simpler testing and deal with needing to transform it into XR later
      val xr = XR.CustomQueryRef(selectClause)
      xr to dynamics
    }
  }
}
