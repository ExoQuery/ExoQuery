package io.exoquery.android

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import io.exoquery.SqlCompiledQuery
import io.exoquery.annotation.ExoInternal
import io.exoquery.sql
import io.exoquery.controller.android.AndroidDatabaseController
import io.exoquery.controller.android.AndroidxArrayWrapper
import io.exoquery.controller.sqlite.Unused
import io.exoquery.controller.toControllerQuery
import io.exoquery.testdata.BasicSchemaTerpal
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

inline fun <reified T> SqlCompiledQuery<T>.toSupportSqliteQuery(ctx: AndroidDatabaseController): SupportSQLiteQuery {
  val paramArray = run {
    val controllerQuery = this.toControllerQuery(serializer<T>())
    with (ctx) {
      val queryParams = AndroidxArrayWrapper(controllerQuery.params.size)
      prepare(queryParams, Unused, controllerQuery.params)
      queryParams
    }
  }
  @OptIn(ExoInternal::class)
  return SimpleSQLiteQuery(this.token.build(), paramArray.array)
}
