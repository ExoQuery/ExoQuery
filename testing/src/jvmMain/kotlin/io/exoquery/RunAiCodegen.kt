package io.exoquery

import io.exoquery.codegen.ai.AgentCallerService
import io.exoquery.codegen.ai.KoogBasedNameProcessor
import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.WorkingDir
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import javax.sql.DataSource
import kotlin.use

object PostgresTestDB {
  fun DataSource.run(sql: String) =
    this.getConnection().use { conn ->
      sql.split(";").map { it.trim() }.filter { !it.isEmpty() }.forEach { sqlSplit ->
        conn.createStatement().use { stmt ->
          stmt.execute(sqlSplit)
        }
      }
    }

  val embeddedPostgres by lazy {
    val started = EmbeddedPostgres.start()
    val postgresScriptsPath = "/testdb/ai-schema.sql"
    val resource = this::class.java.getResource(postgresScriptsPath)
    if (resource == null) throw NullPointerException("The postgres script path `$postgresScriptsPath` was not found")
    val postgresScript = resource.readText()
    println("---------- Postgres Running on: ${started.getJdbcUrl("postgres", "")}")
    started.getPostgresDatabase().run(postgresScript)
    started
  }
}

fun main() {


  val gen = JdbcGenerator.Live(
    LowLevelCodeGeneratorConfig(
      BasicPath.WorkingDir() + "test_gen",
      BasicPath.DotPath("io.exoquery"),
      NameParser.UsingLLM(
        NameParser.TypeOfLLM.Ollama(),
        processor = KoogBasedNameProcessor({println(it)}, AgentCallerService.Live)
      )
    ),
    { PostgresTestDB.embeddedPostgres.getPostgresDatabase().connection }
  )

  gen.run()


}
