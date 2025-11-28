package io.exoquery.jdbc

import io.exoquery.SqlCompiledQuery
import io.exoquery.controller.r2dbc.R2dbcController
import io.exoquery.controller.r2dbc.R2dbcControllers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import io.exoquery.runOn as runOnCommon
import io.r2dbc.spi.ConnectionFactory

// If R2dbcOptions don't need to be set, use the common run implementation
inline suspend fun <reified T> SqlCompiledQuery<T>.runOn(database: R2dbcController) =
  runOnCommon(database, serializer<T>())

suspend fun <T> SqlCompiledQuery<T>.runOn(database: R2dbcController, serializer: KSerializer<T>) =
  runOnCommon(database, serializer)

inline suspend fun <reified T : Any> SqlCompiledQuery<T>.runOnPostgres(connectionFactory: ConnectionFactory) = run {
  val controller = R2dbcControllers.Postgres(connectionFactory = connectionFactory)
  this.runOn(controller, serializer<T>())
}
