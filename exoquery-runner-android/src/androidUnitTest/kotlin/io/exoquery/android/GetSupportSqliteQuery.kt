package io.exoquery.android

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import io.exoquery.SqlCompiledQuery
import io.exoquery.sql
import io.exoquery.controller.android.AndroidDatabaseController
import io.exoquery.controller.android.AndroidxArrayWrapper
import io.exoquery.controller.sqlite.Unused
import io.exoquery.controller.toControllerQuery
import io.exoquery.testdata.BasicSchemaTerpal
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

class GetSupportSqliteQuery {
}

//@Entity(primaryKeys = ["id")
@Serializable
data class Person(val id: Int, val name: String, val age: Int)


    inline fun <reified T> SqlCompiledQuery<T>.toSupportSqliteQuery(ctx: AndroidDatabaseController): SupportSQLiteQuery {
      val paramArray = run {
        val controllerQuery = this.toControllerQuery(serializer<T>())
        with (ctx) {
          val queryParams = AndroidxArrayWrapper(controllerQuery.params.size)
          prepare(queryParams, Unused, controllerQuery.params)
          queryParams
        }
      }
      return SimpleSQLiteQuery(this.token.build(), paramArray.array)
    }

    val ctx = AndroidDatabaseController.fromApplicationContext("empty_database.db", ApplicationProvider.getApplicationContext(), BasicSchemaTerpal)

    fun executeQuery(q: SupportSQLiteQuery): SupportSQLiteQuery {
      val query = sql.select {
        val p = from(Table<Person>())
        p
      }
      return query.buildFor.Sqlite().toSupportSqliteQuery(ctx)
    }


    //@Dao
    //interface RawDao {
    //  @RawQuery(observedEntities = [Person::class])
    //  fun getUsers(SupportSQLiteQuery query): List<Person>
    //}
