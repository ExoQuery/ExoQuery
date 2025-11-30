package io.exoquery.jdbc

import io.exoquery.SqlCompiledBatchAction
import io.exoquery.checkActionKindValidity
import io.exoquery.controller.*
import io.exoquery.controller.jdbc.JdbcController
import io.exoquery.controller.jdbc.JdbcControllers
import io.exoquery.controller.jdbc.JdbcExecutionOptions
import io.exoquery.printing.MessagesRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import javax.sql.DataSource

suspend fun <BatchInput, Input : Any, Output> SqlCompiledBatchAction<BatchInput, Input, Output>.runOn(
  database: JdbcController,
  serializer: KSerializer<Output>,
  options: JdbcExecutionOptions = JdbcExecutionOptions()
): List<Output> = run {
  val dbType = identityDatabaseType(database)
  when (val action = this.toControllerBatchVerb(serializer)) {
    is ControllerBatchAction -> action.runOn(database, options) as List<Output>
    is ControllerBatchActionReturning.Id<Output> -> {
      checkActionKindValidity(this.actionKind, dbType)
      action.runOn(database, options)
    }
    is ControllerBatchActionReturning.Row<Output> -> {
      when {
        database.isH2() ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      action.runOn(database, options)
    }
  }
}

inline suspend fun <BatchInput, Input : Any, reified Output> SqlCompiledBatchAction<BatchInput, Input, Output>.runOn(database: JdbcController, options: JdbcExecutionOptions = JdbcExecutionOptions()) =
  this.runOn(database, serializer<Output>(), options)

inline suspend fun <BatchInput, reified Input : Any, reified Output> SqlCompiledBatchAction<BatchInput, Input, Output>.runOnPostgres(dataSource: DataSource) = run {
  val controller = JdbcControllers.Postgres(dataSource)
  this.runOn(controller, serializer<Output>())
}

// Stream equivalents
suspend fun <BatchInput, Input : Any, Output> SqlCompiledBatchAction<BatchInput, Input, Output>.streamOn(
  database: JdbcController,
  serializer: KSerializer<Output>,
  options: JdbcExecutionOptions = JdbcExecutionOptions()
): Flow<Output> {
  val dbType = identityDatabaseType(database)
  return when (val action = this.toControllerBatchVerb(serializer)) {
    is ControllerBatchAction -> action.runOn(database, options).map { it as Output }.asFlow()
    is ControllerBatchActionReturning.Id<Output> -> {
      checkActionKindValidity(this.actionKind, dbType)
      action.streamOn(database, options)
    }
    is ControllerBatchActionReturning.Row<Output> -> {
      when {
        database.isH2() ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      action.streamOn(database, options)
    }
  }
}

inline suspend fun <BatchInput, Input : Any, reified Output> SqlCompiledBatchAction<BatchInput, Input, Output>.streamOn(database: JdbcController, options: JdbcExecutionOptions = JdbcExecutionOptions()): Flow<Output> =
  this.streamOn(database, serializer<Output>(), options)
