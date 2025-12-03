package io.exoquery.jdbc

import io.exoquery.SqlCompiledAction
import io.exoquery.checkActionKindValidity
import io.exoquery.controller.*
import io.exoquery.controller.jdbc.JdbcController
import io.exoquery.controller.jdbc.JdbcControllers
import io.exoquery.controller.jdbc.JdbcExecutionOptions
import io.exoquery.printing.MessagesRuntime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import javax.sql.DataSource
import io.exoquery.runOn  as runOnCommon

fun JdbcController.isSqlite(): Boolean = this is JdbcControllers.Sqlite
fun JdbcController.isSqlServer(): Boolean = this is JdbcControllers.SqlServer
fun JdbcController.isH2(): Boolean = this is JdbcControllers.H2
fun JdbcController.isMysql(): Boolean = this is JdbcControllers.Mysql

// For functionality that does not require database options, delegate to the common library
inline suspend fun <Input, reified Output> SqlCompiledAction<Input, Output>.runOn(database: JdbcController) = runOnCommon(database)
suspend fun <Input, Output> SqlCompiledAction<Input, Output>.runOn(database: JdbcController, serializer: KSerializer<Output>) = runOnCommon(database, serializer)

// Otherwise need to use more specialized constructors
suspend fun <Input, Output> SqlCompiledAction<Input, Output>.runOn(database: JdbcController, serializer: KSerializer<Output>, options: JdbcExecutionOptions = JdbcExecutionOptions()): Output = run {
  val actionKind = this.actionKind
  val dbType = identityDatabaseType(database)
  when (val action = this.toControllerAction(serializer)) {
    is ControllerAction -> action.runOn(database, options) as Output
    is ControllerActionReturning.Id<Output> -> {
      checkActionKindValidity(actionKind, dbType)
      action.runOn(database, options)
    }
    is ControllerActionReturning.Row<Output> -> {
      when {
        database.isH2() ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      action.runOn(database, options)
    }
  }
}

inline suspend fun <Input, reified Output> SqlCompiledAction<Input, Output>.runOn(database: JdbcController, options: JdbcExecutionOptions = JdbcExecutionOptions()) =
  this.runOn(database, serializer<Output>(), options)

inline suspend fun <reified Input, reified Output> SqlCompiledAction<Input, Output>.runOnPostgres(dataSource: DataSource) = run {
  val controller = JdbcControllers.Postgres(dataSource)
  this.runOn(controller, serializer<Output>())
}

suspend fun <Input, Output> SqlCompiledAction<Input, Output>.streamOn(database: JdbcController, serializer: KSerializer<Output>, options: JdbcExecutionOptions = JdbcExecutionOptions()): Flow<Output> {
  val actionKind = this.actionKind
  val dbType = identityDatabaseType(database)
  return when (val action = this.toControllerAction(serializer)) {
    is ControllerAction ->
      flowOf(action.runOn(database, options)) as Flow<Output>
    is ControllerActionReturning.Id<Output> -> {
      checkActionKindValidity(actionKind, dbType)
      action.streamOn(database)
    }
    is ControllerActionReturning.Row<Output> -> {
      when {
        database.isH2() ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      action.streamOn(database)
    }
  }
}
inline suspend fun <Input, reified Output> SqlCompiledAction<Input, Output>.streamOn(database: JdbcController, options: JdbcExecutionOptions = JdbcExecutionOptions()): Flow<Output> =
  this.streamOn(database, serializer<Output>(), options)
