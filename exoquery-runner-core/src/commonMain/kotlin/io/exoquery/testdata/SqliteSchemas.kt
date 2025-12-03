package io.exoquery.testdata

import io.exoquery.testdata.SqliteSchemaString
import io.exoquery.controller.Controller
import io.exoquery.controller.TerpalSqlInternal
import io.exoquery.controller.TerpalSqlUnsafe
import io.exoquery.controller.runActionsUnsafe
import io.exoquery.controller.sqlite.CallAfterVersion
import io.exoquery.controller.sqlite.TerpalSchema

object BasicSchemaTerpal : TerpalSchema<Unit> {
  override val version: Long = 1
  @OptIn(TerpalSqlUnsafe::class)
  override suspend fun create(driver: Controller<*>): Unit {
    driver.runActionsUnsafe(SqliteSchemaString)
  }

  override suspend fun migrate(
    driver: Controller<*>,
    oldVersion: Long,
    newVersion: Long,
    vararg callbacks: CallAfterVersion
  ) {
  }
}
