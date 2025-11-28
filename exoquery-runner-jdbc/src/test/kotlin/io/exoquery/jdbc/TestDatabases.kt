package io.exoquery.jdbc

import io.exoquery.controller.jdbc.JdbcControllers
import io.exoquery.controller.jdbc.HikariHelper
import io.exoquery.controller.jdbc.JdbcBasicEncoding
import io.exoquery.controller.jdbc.JdbcEncodingConfig
import io.exoquery.testdata.PersonId
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import javax.sql.DataSource
import kotlin.io.readText
import kotlin.jvm.java

object TestDatabases {
  val embeddedPostgres by lazy {
    val started = EmbeddedPostgres.start()
    val postgresScriptsPath = "/db/postgres-schema.sql"
    val resource = this::class.java.getResource(postgresScriptsPath)
    if (resource == null) throw NullPointerException("The postgres script path `$postgresScriptsPath` was not found")
    val postgresScript = resource.readText()
    println("---------- Postgres Running on: ${started.getJdbcUrl("postgres", "")}")
    started.getPostgresDatabase().run(postgresScript)
    started
  }

  val postgres by lazy {
    JdbcControllers.Postgres(
      embeddedPostgres.getPostgresDatabase(),
      encodingConfig =
        JdbcEncodingConfig(
          additionalEncoders =
            JdbcEncodingConfig.Default.additionalEncoders +
              JdbcBasicEncoding.IntEncoder.contramap { id: PersonId -> id.value },
          additionalDecoders =
            JdbcEncodingConfig.Default.additionalDecoders +
              JdbcBasicEncoding.IntDecoder.map { PersonId(it) }
        )
    )
  }

  val mysql by lazy {
    JdbcControllers.Mysql(HikariHelper.makeDataSource("testMysqlDB"))
  }

  val sqlServer by lazy {
    JdbcControllers.SqlServer(HikariHelper.makeDataSource("testSqlServerDB"))
  }

  val h2 by lazy {
    JdbcControllers.H2(HikariHelper.makeDataSource("testH2DB"))
  }

  val sqlite by lazy {
    JdbcControllers.Sqlite(HikariHelper.makeDataSource("testSqliteDB"))
  }

  val oracle by lazy {
    JdbcControllers.Oracle(HikariHelper.makeDataSource("testOracleDB"))
  }
}

fun DataSource.run(sql: String) =
  this.getConnection().use { conn ->
    sql.split(";").map { it.trim() }.filter { !it.isEmpty() }.forEach { sqlSplit ->
      conn.createStatement().use { stmt ->
        stmt.execute(sqlSplit)
      }
    }
  }
