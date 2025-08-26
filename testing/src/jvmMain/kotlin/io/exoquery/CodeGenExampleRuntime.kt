package io.exoquery

import io.exoquery.codegen.ai.KoogBasedNameProcessor
import io.exoquery.codegen.ai.preparedForRuntime
import io.exoquery.codegen.model.LLM
import io.exoquery.codegen.model.NameParser
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.toGenerator
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import javax.sql.DataSource
import kotlin.use

/**
 * This is an example of how to use the ExoQuery CodeGen at runtime.
 * Unlike the compile-time code generation, you can compose Code.Entities
 * using functions, variables, etc... and it does not need to be put together
 * with compile-time-constant values. The disadvantage is that it does not run at
 * compile-time.
 *
 * Note that for runtime code-generation, you explicitly set the directory
 * in which you want the generated code to be written.
 *
 */
fun main() {
  val gen =
    Code.Entities(
      CodeVersion.Fixed("1.1"),
      io.exoquery.generation.DatabaseDriver.Postgres(
        PostgresTestDB.embeddedPostgres.getJdbcUrl("postgres", "")
      ),
      packagePrefix = "io.exoquery",
      nameParser = NameParser.Composite(
        NameParser.UsingLLM(
          //NameParser.TypeOfLLM.Ollama(),
          LLM.OpenAI(),
          processor = KoogBasedNameProcessor.Live({ println(it) })
        ),
        //NameParser.UncapitalizeColumns
      )
    ).toGenerator(
      java.io.File("test_gen").absolutePath
    ).preparedForRuntime()

  gen.run()
}

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
    val postgresScriptsPath = "/testdb/test-schema.sql"
    val resource = this::class.java.getResource(postgresScriptsPath)
    if (resource == null) throw NullPointerException("The postgres script path `$postgresScriptsPath` was not found")
    val postgresScript = resource.readText()
    println("---------- Postgres Running on: ${started.getJdbcUrl("postgres", "")}")
    started.getPostgresDatabase().run(postgresScript)
    started
  }
}
