package io.exoquery

import io.exoquery.annotation.ExoBuildDatabaseSpecific
import io.exoquery.annotation.ExoBuildFunctionLabel
import io.exoquery.annotation.ExoBuildRoomSpecific
import io.exoquery.sql.GenericDialect
import io.exoquery.sql.MySqlDialect
import io.exoquery.sql.PostgresDialect
import io.exoquery.sql.SqlServerDialect

interface BuildFor<T> {
  @ExoBuildDatabaseSpecific(PostgresDialect::class)
  fun Postgres(): T

  @ExoBuildDatabaseSpecific(PostgresDialect::class)
  fun Postgres(@ExoBuildFunctionLabel label: String): T

  @ExoBuildDatabaseSpecific(SqliteDialect::class)
  fun Sqlite(): T

  @ExoBuildDatabaseSpecific(SqliteDialect::class)
  fun Sqlite(@ExoBuildFunctionLabel label: String): T

  @ExoBuildDatabaseSpecific(MySqlDialect::class)
  fun MySql(): T

  @ExoBuildDatabaseSpecific(MySqlDialect::class)
  fun MySql(@ExoBuildFunctionLabel label: String): T

  @ExoBuildDatabaseSpecific(SqlServerDialect::class)
  fun SqlServer(): T

  @ExoBuildDatabaseSpecific(SqlServerDialect::class)
  fun SqlServer(@ExoBuildFunctionLabel label: String): T

  @ExoBuildDatabaseSpecific(H2Dialect::class)
  fun H2(): T

  @ExoBuildDatabaseSpecific(H2Dialect::class)
  fun H2(@ExoBuildFunctionLabel label: String): T

  @ExoBuildRoomSpecific
  fun Room(): Unit

  @ExoBuildRoomSpecific
  fun Room(@ExoBuildFunctionLabel label: String): Unit

  @ExoBuildDatabaseSpecific(GenericDialect::class)
  fun GenericDatabase(): T

  /**
   * When you want to specify the exact type that should come out of the
   * room query function e.g.
   * ```
   * @Query("SELECT * FROM Person")
   * fun myQuery(): Flow<Person>
   * ```
   */
  @ExoBuildRoomSpecific
  fun <T> RoomWithType(): Unit

  @ExoBuildRoomSpecific
  fun <T> RoomWithType(@ExoBuildFunctionLabel label: String): Unit
}
