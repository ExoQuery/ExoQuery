package io.exoquery.controller

import io.exoquery.SqlCompiledAction
import io.exoquery.SqlCompiledBatchAction
import io.exoquery.SqlCompiledQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

suspend fun <T, Session, Stmt, ExecutionOpts> ControllerTransactional<Session, Stmt, ExecutionOpts>.transaction(executionOptions: ExecutionOpts, block: suspend CommonTransactionScope<ExecutionOpts>.() -> T): T =
  @OptIn(TerpalSqlInternal::class)
  withTransactionScope(executionOptions) {
    val coroutineScope = this
    block(CommonTransactionScope<ExecutionOpts>(coroutineScope, this@transaction))
  }

suspend fun <T, Session, Stmt, ExecutionOpts> ControllerTransactional<Session, Stmt, ExecutionOpts>.transaction(block: suspend CommonTransactionScope<ExecutionOpts>.() -> T): T =
  transaction(this.DefaultOpts(), block)

class CommonTransactionScope<ExecutionOpts>(private val scope: CoroutineScope, private val ctxInput: Controller<ExecutionOpts>) {
  @PublishedApi
  internal val ctx: Controller<ExecutionOpts> = ctxInput

  suspend fun <T> SqlCompiledQuery<T>.runOnTransaction(serializer: KSerializer<T>, options: ExecutionOpts = ctx.DefaultOpts()) =
    @OptIn(TerpalSqlInternal::class)
    ctx.run(this.toControllerQuery(serializer), options)

  inline suspend fun <reified T : Any> SqlCompiledQuery<T>.runOnTransaction(options: ExecutionOpts = ctx.DefaultOpts()) =
    this.runOnTransaction(serializer<T>(), options)

  inline suspend fun <Input, reified Output : Any> SqlCompiledAction<Input, Output>.runOnTransaction(serializer: KSerializer<Output>, options: ExecutionOpts = ctx.DefaultOpts()) =
    when (val action = this.toControllerAction(serializer)) {
      is ControllerAction -> action.runOn(ctx) as Output
      is ControllerActionReturning.Id<Output> -> action.runOn(ctx)
      is ControllerActionReturning.Row<Output> -> action.runOn(ctx)
    }

  inline suspend fun <Input, reified Output : Any> SqlCompiledAction<Input, Output>.runOnTransaction(options: ExecutionOpts = ctx.DefaultOpts()) =
    this.runOnTransaction(serializer<Output>(), options)

  inline suspend fun <BatchInput, Input : Any, reified Output> SqlCompiledBatchAction<BatchInput, Input, Output>.runOnTransaction(serializer: KSerializer<Output>, options: ExecutionOpts = ctx.DefaultOpts()) =
    when (val action = this.toControllerBatchVerb(serializer)) {
      is ControllerBatchAction -> action.runOn(ctx) as List<Output>
      is ControllerBatchActionReturning.Id<Output> -> action.runOn(ctx)
      is ControllerBatchActionReturning.Row<Output> -> action.runOn(ctx)
    }

  inline suspend fun <BatchInput, Input : Any, reified Output> SqlCompiledBatchAction<BatchInput, Input, Output>.runOnTransaction(options: ExecutionOpts = ctx.DefaultOpts()) =
    this.runOnTransaction(serializer<Output>(), options)
}
