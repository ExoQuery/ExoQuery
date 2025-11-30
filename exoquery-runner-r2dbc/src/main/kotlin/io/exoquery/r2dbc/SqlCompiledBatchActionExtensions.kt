package io.exoquery.r2dbc

import io.exoquery.SqlCompiledBatchAction
import io.exoquery.checkActionKindValidity
import io.exoquery.controller.*
import io.exoquery.controller.r2dbc.R2dbcController
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.controller.r2dbc.R2dbcExecutionOptions
import io.exoquery.printing.MessagesRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

suspend fun <BatchInput, Input : Any, Output> SqlCompiledBatchAction<BatchInput, Input, Output>.runOn(
  database: R2dbcController,
  serializer: KSerializer<Output>
): List<Output> = run {
  val dbType = identityDatabaseType(database)
  when (val action = this.toControllerBatchVerb(serializer)) {
    is ControllerBatchAction -> action.runOn(database) as List<Output>
    is ControllerBatchActionReturning.Id<Output> -> {
      checkActionKindValidity(this.actionKind, dbType)
      action.runOn(database)
    }
    is ControllerBatchActionReturning.Row<Output> -> {
      when {
        database is R2dbcControllers.H2 ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      action.runOn(database)
    }
  }
}

inline suspend fun <BatchInput, Input : Any, reified Output> SqlCompiledBatchAction<BatchInput, Input, Output>.runOn(
  database: R2dbcController,
  options: R2dbcExecutionOptions = R2dbcExecutionOptions.Default()
) = this.runOn(database, serializer<Output>())

// Stream equivalents
suspend fun <BatchInput, Input : Any, Output> SqlCompiledBatchAction<BatchInput, Input, Output>.streamOn(
  database: R2dbcController,
  serializer: KSerializer<Output>,
  options: R2dbcExecutionOptions = R2dbcExecutionOptions.Default()
): Flow<Output> {
  val dbType = identityDatabaseType(database)
  return when (val action = this.toControllerBatchVerb(serializer)) {
    is ControllerBatchAction -> action.runOn(database).map { it as Output }.asFlow()
    is ControllerBatchActionReturning.Id<Output> -> {
      checkActionKindValidity(this.actionKind, dbType)
      action.streamOn(database)
    }
    is ControllerBatchActionReturning.Row<Output> -> {
      when {
        database is R2dbcControllers.H2 ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      action.streamOn(database)
    }
  }
}

inline suspend fun <BatchInput, Input : Any, reified Output> SqlCompiledBatchAction<BatchInput, Input, Output>.streamOn(
  database: R2dbcController,
  options: R2dbcExecutionOptions = R2dbcExecutionOptions.Default()
): Flow<Output> = this.streamOn(database, serializer<Output>(), options)
