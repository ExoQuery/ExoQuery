package io.exoquery.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.match
import io.exoquery.annotation.WasSterilizedAdHoc
import io.exoquery.parseError
import io.exoquery.plugin.hasAnnotation
import io.exoquery.plugin.trees.*
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetField
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

context(CX.Scope, CX.Builder)
fun IrSimpleFunction.markSterilizedAdHoc() {
  this.annotations = this.annotations + makeLifter().makeSeenAnnotation()
}


context(CX.Scope, CX.Builder)
fun IrSimpleFunction.wasSterilizedAdHoc(): Boolean =
  this.hasAnnotation<WasSterilizedAdHoc>()

context(CX.Scope)
fun IrCall.replaceSingleReturnBodyWith(newReturn: IrExpression) {
  val call = this
  val body = this.symbol.owner.replaceSingleReturnBodyWith(newReturn)
}

context(CX.Scope)
fun IrSimpleFunction.replaceSingleReturnBodyWith(newReturn: IrExpression) {
  val nonNullBody = body ?: parseError("The function body is null. This should not be possible here:\n${this.symbol.owner.dumpKotlinLike()}", this)

  if (nonNullBody is IrBlockBody) {
    val functionReturn = (nonNullBody.statements[0] as IrReturn)
    functionReturn.value = newReturn
  } else if (nonNullBody is IrExpressionBody) {
    nonNullBody.expression = newReturn
  } else {
    parseError("The function body is not a block or expression body. This should not be possible here:\n${this.symbol.owner.dumpKotlinLike()}", this)
  }
}

context(CX.Scope)
fun IrGetField.replaceSingleReturnBodyWith(newReturn: IrExpression) {
  val field = this
  val body = field.symbol.owner.initializer ?: parseError("The field initializer is null. This should not be possible here:\n${field.symbol.owner.dumpKotlinLike()}", this)
  field.symbol.owner.initializer!!.expression = newReturn
}

context(CX.Scope)
fun IrField.replaceInitializerBodyWith(newReturn: IrExpression) {
  val notNullBody = initializer ?: parseError("The field initializer is null. This should not be possible here:\n${this.dumpKotlinLike()}", this)
  notNullBody.expression = newReturn
  //this.initializer = notNullBody
}

context(CX.Scope)
fun IrVariable.replaceInitializerBodyWith(newReturn: IrExpression) {
  initializer = newReturn
}

context(CX.Scope)
fun IrField.findInitializerExpression() =
  this.match(
    case(Ir.Field[Is(), Ir.Call[Is()]]).then { _, (expr) -> expr }
  )

context(CX.Scope)
fun IrVariable.findInitializerExpression() =
  this.match(
    case(Ir.Variable[Is(), Ir.Call[Is()]]).then { _, (expr) -> expr }
  )

context(CX.Scope)
fun IrExpression.uprootableQueryOrNull(): SqlQueryExpr.Uprootable? =
  this.match(
    case(SqlQueryExpr.Uprootable[Is()]).then { uprootable ->
      uprootable
    }
  )

context(CX.Scope)
fun IrExpression.withUprootableQueryOrNull(): Pair<IrExpression, SqlQueryExpr.Uprootable>? =
  this.uprootableQueryOrNull()?.let { uprootable -> this to uprootable }


context(CX.Scope)
fun IrExpression.uprootableExpressionOrNull(): SqlExpressionExpr.Uprootable? =
  this.match(
    case(SqlExpressionExpr.Uprootable[Is()]).then { uprootable ->
      uprootable
    }
  )

context(CX.Scope)
fun IrExpression.withUprootableExpressionOrNull(): Pair<IrExpression, SqlExpressionExpr.Uprootable>? =
  this.uprootableExpressionOrNull()?.let { uprootable -> this to uprootable }

context(CX.Scope)
fun IrExpression.uprootableActionOrNull(): SqlActionExpr.Uprootable? =
  this.match(
    case(SqlActionExpr.Uprootable[Is()]).then { uprootable ->
      uprootable
    }
  )

context(CX.Scope)
fun IrExpression.withUprootableActionOrNull(): Pair<IrExpression, SqlActionExpr.Uprootable>? =
  this.uprootableActionOrNull()?.let { uprootable -> this to uprootable }

context(CX.Scope)
fun IrExpression.uprootableBatchActionOrNull(): SqlBatchActionExpr.Uprootable? =
  this.match(
    case(SqlBatchActionExpr.Uprootable[Is()]).then { batchAction ->
      batchAction
    }
  )

context(CX.Scope)
fun IrExpression.withUprootableBatchActionOrNull(): Pair<IrExpression, SqlBatchActionExpr.Uprootable>? =
  this.uprootableBatchActionOrNull()?.let { batchAction -> this to batchAction }
