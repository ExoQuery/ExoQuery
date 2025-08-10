package io.exoquery.codegen.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.NameProcessorLLM
import io.exoquery.codegen.model.TablePrepared
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow

/**
 * IMPORTANT! This module should NOT be directly referenced anywhere
 * else in exoquery-engine. It should only be invoked either by the exoquery
 * compiler plugin or by a user who explicitly wants to use it.
 * In in the former case (the most typical) the gradle block:
 * ```kotlin
 * exoQuery {
 *   enableLlmNaming = true
 * }
 * ```
 * needs to be specified which will instruct ExoQuery to add Koog to the compile-time classpath.
 * If the user wants to use this module directly (e.g. for testing purposes) they need
 * to explicitly include Koog in their gradle dependencies list.
 */
class KoogBasedNameProcessor(val log: (String) -> Unit = {}): NameProcessorLLM {
  class KoogCodegenError(message: String): Exception(message)

  override suspend fun processTables(usingLLM: NameParser.UsingLLM, tables: List<TablePrepared>): List<TablePrepared> {
    val (columnLabels, tableLabels) = columnAndTableLists(tables)
    val preparedAgentInputs =
      produceInputs(columnLabels, usingLLM.maxColumnsPerCall) + produceInputs(tableLabels, usingLLM.maxTablesPerCall)

    val (agent, name) = makeAgent(usingLLM)
    val mappings = executeAgentAndParse(agent, name, preparedAgentInputs, 5)

    val renamedTables =
      tables.map { table ->
        val renamedColumns = table.columns.map { column ->
          val newName = mappings[column.name] ?: column.name
          column.copy(name = newName)
        }
        table.copy(name = mappings[table.name] ?: table.name, columns = renamedColumns)
      }

    return renamedTables
  }

  private fun columnAndTableLists(tables: List<TablePrepared>): Pair<List<String>, List<String>> {
    val columnLabels = tables.flatMap { it.columns }.map { it.name }.distinct().sorted()
    val tableLabels = tables.map { it.name }.distinct().sorted()
    return Pair(columnLabels, tableLabels)
  }

  /**
   * Produce the input prompt for a column/table name list, and separate into chunks
   * It should look like:
   * ```
   * <Input>
   * column1
   * column2
   * ...
   * </Input>
   */
  private fun produceInputs(nameList: List<String>, itemsPerList: Int): List<String> =
    if (nameList.isEmpty()) emptyList()
    else {
      nameList.chunked(itemsPerList).map {
        val columns = it.joinToString("\n") { name -> name.trim() }
        "<Input>\n$columns\n</Input>"
      }
    }



  /**
   * Should start with <Output> and end with </Output>.
   * Contains the list of pairs of old label and new label. So for example:
   * ```
   * <Output>
   * oldColumn1:NewColumn1
   * oldColumn2:NewColumn2
   * </Output>
   */
  private fun parseOutput(output: String): List<Pair<String, String>> {
    if (!output.startsWith("<Output>") || !output.endsWith("</Output>")) {
      throw KoogCodegenError("Output should start with <Output> and end with </Output>. Got: $output")
    }
    val content = output.removePrefix("<Output>").removeSuffix("</Output>").trim()
    return content.lines().map { line ->
      val parts = line.split(":")
      if (parts.size != 2) {
        throw KoogCodegenError("Each line in the output should contain exactly one ':' character. Got: $line")
      }
      parts[0].trim() to parts[1].trim()
    }
  }

  /**
   * NOTE: If I want to optimize this with a sequence, sequence.flatMap is not inline so I would need to do something like this:
   * ```
   *  for (batch in inputs.chunked(numParallel)) {
   *     val deferreds = batch.map { req -> async { model.run(req) } }
   *     deferreds.awaitAll().forEach { send(it) }
   *   }
   * ```
   */
  private suspend fun executeAgentAndParse(model: AIAgent<String, String>, modelName: String, inputs: List<String>, numParallel: Int): Map<String, String> = coroutineScope {
    val chunkedInputs = inputs.chunked(numParallel)
    val output =
      chunkedInputs.withIndex().flatMap { (i, batch) ->
        log("Processing batch ${i}/${chunkedInputs.size} with model ${modelName}")
        val agentOutput = batch.map { request ->
          async {
            model.run(request)
          }
        }.awaitAll()
        // Parse the output of the agent as we go, if anything goes wrong immediately throw an error
        agentOutput.map { parseOutput(it) }
      }.flatten().toMap()
    log("Completed processing model ${modelName}")
    output
  }



  private fun makeAgent(usingLLM: NameParser.UsingLLM): Pair<AIAgent<String, String>, String> = run {
    when(usingLLM.type) {
      is NameParser.TypeOfLLM.Ollama -> {
        val (model, name) = makeOllamaModel(usingLLM.type)
        AIAgent(
          executor = simpleOllamaAIExecutor(usingLLM.type.url),
          systemPrompt = usingLLM.systemPrompt,
          llmModel = model,
          temperature = 0.0
        ) to name
      }
      is NameParser.TypeOfLLM.OpenAI -> {
        val (model, name) = makeOpenAIModel(usingLLM.type)
        val apiKey = usingLLM.type.apiKey ?: throw KoogCodegenError(
          "OpenAI API key is not provided. Please specify it using TypeOfLLM.OpenAI.apiKey, TypeOfLLM.OpenAI.apiKeyEnvVar or the `api-key` field of your codegen config (.codegen.properties by default)."
        )
        AIAgent(
          executor = simpleOpenAIExecutor(apiKey),
          systemPrompt = usingLLM.systemPrompt,
          llmModel = model,
          temperature = 0.0
        ) to name
      }
    }
  }

  private fun makeOllamaModel(ollamaType: NameParser.TypeOfLLM.Ollama) = run {
    val model = LLModel(
      provider = LLMProvider.Ollama,
      id = ollamaType.model,
      capabilities = listOf(
        LLMCapability.Temperature,
        LLMCapability.Schema.JSON.Simple
      )
    )
    model to model.id
  }

  private val supportedOpenAiModels =
    listOf(
      OpenAIModels.Reasoning.O1,
      OpenAIModels.Reasoning.O3,
      OpenAIModels.Reasoning.O1Mini,
      OpenAIModels.Reasoning.O3Mini,
      OpenAIModels.Reasoning.GPT4oMini,
      OpenAIModels.CostOptimized.GPT4_1Nano
    )

  private fun makeOpenAIModel(openAiType: NameParser.TypeOfLLM.OpenAI) = run {
    val model =
      supportedOpenAiModels.find { it.id == openAiType.model }
      ?: throw KoogCodegenError("Unsupported OpenAI model: ${openAiType.model}. Supported models are: ${supportedOpenAiModels.joinToString(", ") { it.id }}")
    model to model.id
  }


}
