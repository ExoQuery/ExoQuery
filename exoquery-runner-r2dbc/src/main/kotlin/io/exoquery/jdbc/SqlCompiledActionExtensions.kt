package io.exoquery.jdbc

import io.exoquery.SqlCompiledAction
import io.exoquery.checkActionKindValidity
import io.exoquery.controller.*
import io.exoquery.controller.r2dbc.R2dbcController
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.controller.r2dbc.R2dbcExecutionOptions
import io.exoquery.printing.MessagesRuntime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import io.exoquery.runOn as runOnCommon
import io.r2dbc.spi.ConnectionFactory

fun R2dbcController.isSqlServer(): Boolean = this is R2dbcControllers.SqlServer
fun R2dbcController.isH2(): Boolean = this is R2dbcControllers.H2
fun R2dbcController.isMysql(): Boolean = this is R2dbcControllers.Mysql

// For functionality that does not require database options, delegate to the common library
inline suspend fun <Input, reified Output> SqlCompiledAction<Input, Output>.runOn(database: R2dbcController) = runOnCommon(database)
suspend fun <Input, Output> SqlCompiledAction<Input, Output>.runOn(database: R2dbcController, serializer: KSerializer<Output>) = runOnCommon(database, serializer)

// Otherwise need to use more specialized constructors
suspend fun <Input, Output> SqlCompiledAction<Input, Output>.runOn(
  database: R2dbcController,
  serializer: KSerializer<Output>,
  options: R2dbcExecutionOptions = R2dbcExecutionOptions.Default()
): Output = run {
  val actionKind = this.actionKind
  val dbType = identityDatabaseType(database)
  when (val action = this.toControllerAction(serializer)) {
    is ControllerAction -> action.runOn(database) as Output
    is ControllerActionReturning.Id<Output> -> {
      checkActionKindValidity(actionKind, dbType)
      action.runOn(database)
    }
    is ControllerActionReturning.Row<Output> -> {
      when {
        database.isH2() ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      action.runOn(database)
    }
  }
}

inline suspend fun <Input, reified Output> SqlCompiledAction<Input, Output>.runOn(
  database: R2dbcController,
  options: R2dbcExecutionOptions = R2dbcExecutionOptions.Default()
) = this.runOn(database, serializer<Output>(), options)

inline suspend fun <reified Input, reified Output> SqlCompiledAction<Input, Output>.runOnPostgres(connectionFactory: ConnectionFactory) = run {
  val controller = R2dbcControllers.Postgres(connectionFactory = connectionFactory)
  this.runOn(controller, serializer<Output>())
}
