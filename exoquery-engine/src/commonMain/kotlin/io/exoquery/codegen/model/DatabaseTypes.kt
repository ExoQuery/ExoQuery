package io.exoquery.codegen.model

import io.exoquery.lang.SqlIdiom
import kotlin.reflect.KClass

// Kotlin:
object DatabaseTypes {
  val all: List<DatabaseType> = listOf(H2, MySql, SqlServer, Postgres, Sqlite) //Oracle

  fun fromProductName(productName: String): DatabaseType {
    return all.find { it.databaseName == productName }
      ?: throw IllegalArgumentException(
        "Database type $productName not supported. Possible Values are: ${all.map { it.databaseName }}"
      )
  }

  sealed interface DatabaseType {
    val databaseName: String
  }

  object H2 : DatabaseType {
    override val databaseName: String = "H2"
  }

  object MySql : DatabaseType {
    override val databaseName: String = "MySQL"
  }

  object SqlServer : DatabaseType {
    override val databaseName: String = "Microsoft SQL Server"
  }

  object Postgres : DatabaseType {
    override val databaseName: String = "PostgreSQL"
  }

  object Sqlite : DatabaseType {
    override val databaseName: String = "SQLite"
  }

  // Unknown database is technically allowed, assume it has a postgres dialect
  data class Unknown(override val databaseName: String) : DatabaseType

  //object Oracle : DatabaseType {
  //  override val databaseName: String = "Oracle"
  //  override val dialect: KClass<out SqlIdiom> = io.exoquery.sql.OracleDialect::class
  //}

}
