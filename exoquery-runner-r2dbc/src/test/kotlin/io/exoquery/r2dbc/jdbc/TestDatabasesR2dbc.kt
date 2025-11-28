package io.exoquery.r2dbc.jdbc

import io.exoquery.controller.r2dbc.R2dbcBasicEncoding
import io.exoquery.controller.r2dbc.R2dbcController
import io.exoquery.controller.r2dbc.R2dbcControllers
import io.exoquery.controller.r2dbc.R2dbcEncodingConfig
import io.exoquery.testdata.PersonId
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres

object TestDatabasesR2dbc {
  val embeddedPostgres: EmbeddedPostgres by lazy {
    val started = EmbeddedPostgres.builder().start()
    val postgresScriptsPath = "/db/postgres-schema.sql"
    val resource = this::class.java.getResource(postgresScriptsPath)
    requireNotNull(resource) { "The postgres script path `$postgresScriptsPath` was not found" }
    val postgresScript = resource.readText()
    started.postgresDatabase.connection.use { conn ->
      val commands = postgresScript.split(';')
      commands.filter { it.isNotBlank() }.forEach { cmd ->
        conn.prepareStatement(cmd).execute()
      }
    }
    started
  }

  val PostgresContextForEncoding: R2dbcController by lazy {
    R2dbcControllers.Postgres(
      R2dbcEncodingConfig.Default(
        setOf(R2dbcBasicEncoding.IntEncoder.contramap { id: PersonId -> id.value }),
        setOf(R2dbcBasicEncoding.IntDecoder.map { PersonId(it) })
      ),
      postgres
    )
  }

  val postgres: ConnectionFactory by lazy {
    val ep = embeddedPostgres
    val host = "localhost"
    val port = ep.port
    val db = "postgres"
    val user = "postgres"
    ConnectionFactories.get(
      ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, "postgresql")
        .option(ConnectionFactoryOptions.HOST, host)
        .option(ConnectionFactoryOptions.PORT, port)
        .option(ConnectionFactoryOptions.DATABASE, db)
        .option(ConnectionFactoryOptions.USER, user)
        .build()
    )
  }

  val sqlServer: ConnectionFactory by lazy {
    ConnectionFactories.get(
      ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, "mssql")
        .option(ConnectionFactoryOptions.HOST, "localhost")
        .option(ConnectionFactoryOptions.PORT, 31433)
        .option(ConnectionFactoryOptions.DATABASE, "exoquery_test")
        .option(ConnectionFactoryOptions.USER, "sa")
        .option(ConnectionFactoryOptions.PASSWORD, "ExoQueryRocks!")
        .build()
    )
  }

  val mysql: ConnectionFactory by lazy {
    ConnectionFactories.get(
      ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, "mysql")
        .option(ConnectionFactoryOptions.HOST, "localhost")
        .option(ConnectionFactoryOptions.PORT, 33306)
        .option(ConnectionFactoryOptions.DATABASE, "exoquery_test")
        .option(ConnectionFactoryOptions.USER, "root")
        .option(ConnectionFactoryOptions.PASSWORD, "root")
        .build()
    )
  }

  val h2: ConnectionFactory by lazy {
    ConnectionFactories.get("r2dbc:h2:mem:///exoquery_test;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT%20FROM%20'classpath:db/h2-schema.sql'")
  }

  val oracle: ConnectionFactory by lazy {
    ConnectionFactories.get(
      ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, "oracle")
        .option(ConnectionFactoryOptions.HOST, "localhost")
        .option(ConnectionFactoryOptions.PORT, 31521)
        .option(ConnectionFactoryOptions.DATABASE, "xe")
        .option(ConnectionFactoryOptions.USER, "exoquery_test")
        .option(ConnectionFactoryOptions.PASSWORD, "ExoQueryRocks!")
        .build()
    )
  }
}
