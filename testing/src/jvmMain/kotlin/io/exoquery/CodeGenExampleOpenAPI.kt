package io.exoquery

/**
 * In order to use this example, you need to set your OpenAI API key.
 * You can either do this by adding a .codegen.properties file in the root of your project
 * that has an api-key property
 * ```
 * # .codegen.properties
 * api-key=your-openai-api-key
 * ```
 * Or you can use a specified environment variable:
 * ```
 * LLM.OpenAI(LLM.OpenAI(apiKeyEnvVar = "OPENAI_API_KEY"))
 * // The set OPENAI_API_KEY=your-openai-api-key in your environment
 * ```
 */
fun main() {
  //val cc = sql.generateAndReturn(
  //  Code.Entities(
  //    CodeVersion.Fixed("1.5"),
  //    DatabaseDriver.Postgres("jdbc:postgresql://localhost:5432/postgres"),
  //    packagePrefix = "io.exoquery",
  //    username = "postgres",
  //    password = "postgres",
  //    nameParser =
  //      NameParser.Composite(
  //        NameParser.UsingLLM(
  //          LLM.OpenAI()
  //        ),
  //        NameParser.UncapitalizeColumns
  //      ),
  //    detailedLogs = true
  //  )
  //)
  //println(pprint(cc))
}
