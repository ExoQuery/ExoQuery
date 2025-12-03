package io.exoquery.jdbc

import io.exoquery.SqlCompiledAction
import io.exoquery.SqlCompiledBatchAction
import io.exoquery.SqlCompiledQuery
import io.exoquery.annotation.ExoInternal
import io.exoquery.checkActionKindValidity
import io.exoquery.controller.*
import io.exoquery.controller.jdbc.JdbcController
import io.exoquery.controller.jdbc.JdbcControllers
import io.exoquery.controller.jdbc.JdbcExecutionOptions
import io.exoquery.controller.jdbc.RawColumnSet
import io.exoquery.printing.MessagesRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import javax.sql.DataSource

@ExoInternal
@OptIn(TerpalSqlInternal::class)
suspend fun <Input, Output> SqlCompiledAction<Input, Output>.runBothOn(database: JdbcController, serializer: KSerializer<Output>, options: JdbcExecutionOptions = JdbcExecutionOptions()): List<Pair<Output, RawColumnSet>> = run {
  val actionKind = this.actionKind
  val dbType = identityDatabaseType(database)
  when (val action = this.toControllerAction(serializer)) {
    is ControllerAction -> listOf(database.run(action, options)) as List<Pair<Output, RawColumnSet>>
    is ControllerActionReturning.Id<Output> -> {
      checkActionKindValidity(actionKind, dbType)
      database.streamBoth(action, options).toList()
    }
    is ControllerActionReturning.Row<Output> -> {
      when {
        database.isH2() ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      database.streamBoth<Output>(action, options).toList()
    }
  }
}

@ExoInternal
@OptIn(TerpalSqlInternal::class)
inline suspend fun <Input, reified Output> SqlCompiledAction<Input, Output>.runBothOn(database: JdbcController, options: JdbcExecutionOptions = JdbcExecutionOptions()): List<Pair<Output, RawColumnSet>> =
  this.runBothOn(database, serializer<Output>(), options)

@ExoInternal
@OptIn(TerpalSqlInternal::class)
suspend fun <BatchInput, Input : Any, Output> SqlCompiledBatchAction<BatchInput, Input, Output>.runOnBoth(
  database: JdbcController,
  serializer: KSerializer<Output>,
  options: JdbcExecutionOptions = JdbcExecutionOptions()
): List<Pair<Output, RawColumnSet>> = run {
  val dbType = identityDatabaseType(database)
  when (val action = this.toControllerBatchVerb(serializer)) {
    is ControllerBatchAction -> listOf(database.run(action, options)) as List<Pair<Output, RawColumnSet>>
    is ControllerBatchActionReturning.Id<Output> -> {
      checkActionKindValidity(this.actionKind, dbType)
      database.streamBoth(action, options).toList()
    }
    is ControllerBatchActionReturning.Row<Output> -> {
      when {
        database.isH2() ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      database.streamBoth<Output>(action, options).toList()
    }
  }
}

@ExoInternal
@OptIn(TerpalSqlInternal::class)
suspend fun <T> SqlCompiledQuery<T>.runBothOn(database: JdbcController, serializer: KSerializer<T>, options: JdbcExecutionOptions) =
  database.runBoth(this.toControllerQuery(serializer), options)
