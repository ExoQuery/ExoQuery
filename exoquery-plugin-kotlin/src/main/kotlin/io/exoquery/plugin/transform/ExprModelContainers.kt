package io.exoquery.plugin.transform

import io.exoquery.*
import io.exoquery.plugin.trees.Lifter
import io.exoquery.plugin.trees.PT
import io.exoquery.plugin.trees.simpleTypeArgs
import io.exoquery.lang.Token
import io.exoquery.lang.token
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrExpression


data class SqlCompiledQueryExpr(
  // The SqlQuery instance that this represents a SqlCompiledQuery from
  val sqlQueryExpr: IrExpression,
  val queryString: String,
  val queryTokenized: Token,
  val needsTokenization: Boolean,
  val label: String?,
  val phase: Phase,
  val originalEncodedQueryXR: String,
  val originalEncodedQueryModel: String
) {
  context(scope: CX.Scope, builder: CX.Builder)
  fun plant(): IrExpression {
    val lifter = Lifter(builder)

    // Gete the type T of the SqlQuery<T> that .build is called on
    val queryOutputType = sqlQueryExpr.type.simpleTypeArgs[0]
    // i.e. this is the SqlQuery.params call
    val callParamsFromSqlQuery = sqlQueryExpr.callDispatch("params").invoke()

    return with(lifter) {
      val labelExpr = if (label != null) label.lift() else irBuilder.irNull()
      makeWithTypes<SqlCompiledQuery<*>>(
        listOf(queryOutputType),
        listOf(
          queryString.lift(), // value
          createLambda0(queryTokenized.token.lift(callParamsFromSqlQuery), scope.currentDeclarationParentOrFail()), // token
          // If there are no ParamMulti values then we know that we can use the original query built with the .build function.
          // Now we can't check what the values in ParamSet are but we have this value in the TagForParam.paramType field
          // which shows us if the Param is a ParamSingle or ParamMulti. We need to check that in the AST in order to know that this
          // value is supposed to be.
          irBuilder.irBoolean(needsTokenization), // needsTokenization
          labelExpr,
          make<SqlCompiledQuery.DebugData>(
            Phase.CompileTime.lift(),
            call(PT.io_exoquery_unpackQueryLazy).invoke(builder.builder.irString(originalEncodedQueryXR)),
            call(PT.io_exoquery_unpackQueryModelLazy).invoke(builder.builder.irString(originalEncodedQueryModel))
          )
        )
      )
    }
  }
}

// Same thing as TransformCompileQuery buildContainerCompiletime method
data class SqlCompiledActionExpr(
  val sqlActionExpr: IrExpression,
  val queryString: String,
  val queryTokenized: Token,
  val actionKind: ActionKind,
  val actionReturningKind: ActionReturningKind,
  val label: String?,
  val phase: Phase,
  val originalEncodedActionXR: String
) {
  context(scope: CX.Scope, builder: CX.Builder)
  fun plant(): IrExpression {
    val lifter = Lifter(builder)
    val callParamsFromSqlAction = sqlActionExpr.callDispatch("params").invoke()
    val inputType = sqlActionExpr.type.simpleTypeArgs[0]
    val outputType = sqlActionExpr.type.simpleTypeArgs[1]

    return with(lifter) {
      val labelExpr = if (label != null) label.lift() else irBuilder.irNull()
      makeWithTypes<SqlCompiledAction<*, *>>(
        listOf(inputType, outputType),
        listOf(
          queryString.lift(), // value
          queryTokenized.token.lift(callParamsFromSqlAction), // token
          // If there are no ParamMulti values then we know that we can use the original query built with the .build function.
          // Now we can't check what the values in ParamSet are but we have this value in the TagForParam.paramType field
          // which shows us if the Param is a ParamSingle or ParamMulti. We need to check that in the AST in order to know that this
          // value is supposed to be.
          irBuilder.irBoolean(false), // needsTokenization (todo need to determine this from the tokenized value i.e. only `true` if there are no ParamMulti values)
          actionKind.lift(),
          actionReturningKind.lift(),
          labelExpr,
          make<SqlCompiledAction.DebugData>(
            Phase.CompileTime.lift(),
            call(PT.io_exoquery_unpackActionLazy).invoke(builder.builder.irString(originalEncodedActionXR))
          )
        )
      )
    }
  }
}

data class SqlCompiledBatchActionExpr(
  val sqlBatchActionExpr: IrExpression,
  val queryString: String,
  val queryTokenized: Token,
  val actionKind: ActionKind,
  val actionReturningKind: ActionReturningKind,
  val label: String?,
  val phase: Phase,
  val originalEncodedBatchActionXR: String
) {
  context(scope: CX.Scope, builder: CX.Builder)
  fun plant(): IrExpression {
    val lifter = Lifter(builder)
    val callParamsFromSqlBatchAction = sqlBatchActionExpr.callDispatch("params").invoke()
    val callBatchParamFromSqlBatchAction = sqlBatchActionExpr.callDispatch("batchParam").invoke()
    val inputType = sqlBatchActionExpr.type.simpleTypeArgs[0]
    val outputType = sqlBatchActionExpr.type.simpleTypeArgs[1]

    return with(lifter) {
      val labelExpr = if (label != null) label.lift() else irBuilder.irNull()
      makeWithTypes<SqlCompiledBatchAction<*, *, *>>(
        listOf(inputType, outputType),
        listOf(
          queryString.lift(), // value
          queryTokenized.token.lift(callParamsFromSqlBatchAction), // token
          // If there are no ParamMulti values then we know that we can use the original query built with the .build function.
          // Now we can't check what the values in ParamSet are but we have this value in the TagForParam.paramType field
          // which shows us if the Param is a ParamSingle or ParamMulti. We need to check that in the AST in order to know that this
          // value is supposed to be.
          irBuilder.irBoolean(false), // needsTokenization (todo need to determine this from the tokenized value i.e. only `true` if there are no ParamMulti values)
          actionKind.lift(),
          actionReturningKind.lift(),
          callBatchParamFromSqlBatchAction,
          labelExpr,
          make<SqlCompiledBatchAction.DebugData>(
            Phase.CompileTime.lift(),
            call(PT.io_exoquery_unpackBatchActionLazy).invoke(builder.builder.irString(originalEncodedBatchActionXR))
          )
        )
      )
    }
  }
}
