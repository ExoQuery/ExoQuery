package io.exoquery

import io.exoquery.controller.*
import io.exoquery.printing.MessagesRuntime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.serializer

fun checkActionKindValidity(actionKind: ActionKind, dbType: ControllerDatabaseType) =
  when {
    actionKind.isUpdateOrDelete() && dbType.isSqlite() ->
      throw IllegalSqlOperation("SQLite does not support returning ids with returningKeys in UPDATE and DELETE queries. Use .returning instead to add a RETRUNING clause to the query.\n${MessagesRuntime.ReturningExplanation}")
    actionKind.isUpdateOrDelete() && dbType.isSqlServer() ->
      throw IllegalSqlOperation("SQL Server does not support returning ids with returningKeys in UPDATE and DELETE queries. Use .returning instead to add a OUTPUT clause to the query.\n${MessagesRuntime.ReturningExplanation}")
    actionKind.isDelete() && dbType.isH2() ->
      throw IllegalSqlOperation("H2 only supports the `returningKeys` construct with INSERT and UPDATE queries (and H2 does not support `retruning` at all).\n${MessagesRuntime.ReturningExplanation}")
    else ->
      Unit
  }

/**
 * In some cases it is not safe to just cast the Long queryOutput to the expected serializable type
 * because the wrong serializer may be passed in. This function accounts for that.
 * (Generally we only need to account for differing primitive types, not any kind of object possible.)
 */
fun <Output> matchUpOutput(queryOutput: Long, serializer: KSerializer<Output>): Output =
  when (serializer.descriptor.kind) {
    is PrimitiveKind.LONG -> queryOutput as Output
    is PrimitiveKind.INT -> queryOutput.toInt() as Output
    is PrimitiveKind.SHORT -> queryOutput.toShort() as Output
    is PrimitiveKind.BYTE -> queryOutput.toByte() as Output
    is PrimitiveKind.CHAR -> queryOutput.toInt().toChar() as Output
    is PrimitiveKind.FLOAT -> queryOutput.toFloat() as Output
    is PrimitiveKind.DOUBLE -> queryOutput.toDouble() as Output
    is PrimitiveKind.STRING -> queryOutput.toString() as Output
    else -> error("Invalid serializer descriptor: ${serializer.descriptor}. The returningKeys call needs to be a primitive type when using Sqlite Native.")
  }

suspend fun <Input, Output> SqlCompiledAction<Input, Output>.runOn(database: Controller<*>, serializer: KSerializer<Output>): Output = run {
  val dbType = identityDatabaseType(database)
  when (val action = this.toControllerAction(serializer)) {

    is ControllerAction -> {
      val queryOutput = action.runOn(database)
      matchUpOutput(queryOutput, serializer)
    }
    is ControllerActionReturning.Id<*> -> {
      checkActionKindValidity(actionKind, dbType)
      val queryOutput = action.runOn(database)
      if (queryOutput is Long) {
        matchUpOutput(queryOutput, serializer)
      } else {
        queryOutput as Output
      }
    }
    is ControllerActionReturning.Row<Output> -> {
      when {
        dbType.isH2() ->
          throw IllegalStateException("H2 Server does not support the action.returning(...) API. Only `returningKeys` can be used with H2 and only in INSERT and UPDATE queries.\n${MessagesRuntime.ReturningExplanation}")
      }
      action.runOn(database)
    }
  }
}

inline suspend fun <Input, reified Output> SqlCompiledAction<Input, Output>.runOn(database: Controller<*>) =
  this.runOn(database, serializer<Output>())
