package io.exoquery.codegen.ai

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import io.exoquery.codegen.ai.KoogBasedNameProcessor.KoogCodegenError
import io.exoquery.codegen.model.GeneratorBase
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.LLM
import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.NameProcessorLLM
import io.exoquery.codegen.model.NameProcessorLLM.ModelInput
import io.exoquery.codegen.model.ProcessingContext
import io.exoquery.codegen.model.TablePrepared
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.util.UUID

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
abstract class KoogBasedNameProcessor(val log: (String) -> Unit = {}): NameProcessorLLM {
  abstract val agentCaller: AgentCallerService

  class KoogCodegenError(message: String): Exception(message)

  override fun processTables(usingLLM: NameParser.UsingLLM, tables: List<TablePrepared>, ctx: ProcessingContext): List<TablePrepared> =
    runBlocking {
      processTablesBlocking(usingLLM, tables, ctx)
    }

  private suspend fun processTablesBlocking(usingLLM: NameParser.UsingLLM, tables: List<TablePrepared>, ctx: ProcessingContext): List<TablePrepared> {
    val (columnLabels, tableLabels) = columnAndTableLists(tables)

    val requestTables = RequesterFactory(usingLLM.type, usingLLM.systemPromptTables, ctx.rootLevelOpenApiKey)
    val requestColumns = RequesterFactory(usingLLM.type, usingLLM.systemPromptColumns, ctx.rootLevelOpenApiKey)

    val mappingsTables = executeAgentAndParse(requestTables, tableLabels, usingLLM.maxTablesPerCall, 5, ctx.logDetails, "tables")
    val mappingsColumns = executeAgentAndParse(requestColumns, columnLabels, usingLLM.maxColumnsPerCall, 5, ctx.logDetails, "columns")

    val renamedTables =
      tables.map { table ->
        val renamedColumns = table.columns.map { column ->
          val newName = mappingsColumns[column.name] ?: column.name
          column.copy(name = newName)
        }
        table.copy(name = mappingsTables[table.name] ?: table.name, columns = renamedColumns)
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
  private fun produceInputs(nameList: List<String>, itemsPerList: Int): List<ModelInput> =
    if (nameList.isEmpty()) emptyList()
    else {
      nameList.chunked(itemsPerList).map {
        val columns = it.withIndex().joinToString("\n") { (i, name) -> "${i+1})${name.trim()}" }
        ModelInput(it, "<INPUT>\n$columns\n</INPUT>")
      }
    }

  private val OutputTag = "<output>"
  private val OutputTagEnd = "</output>"

  /**
   * Should start with <Output> and end with </Output>.
   * Contains the list of triples `index)oldColumnName:newColumnName`
   *
   * ```
   * <Output>
   * 1)oldColumn1:NewColumn1
   * 2)oldColumn2:NewColumn2
   * </Output>
   */
  private fun parseOutput(originalLabels: List<String>, output: String): List<Pair<String, String>> {
    if (!output.startsWith(OutputTag, ignoreCase = true) || !output.endsWith(OutputTagEnd, ignoreCase = true)) {
      throw KoogCodegenError("Output should start with ${OutputTag} and end with ${OutputTagEnd} (case-insensitive). Got: $output")
    }
    val content = output.drop(OutputTag.length).dropLast(OutputTagEnd.length).trim()
    // Split `index)oldColumnName:newColumnName` into triples.
    // First get the index, then the other two parts.
    val lines =
      content.lines().map { line ->
        val indexAndKV = line.split(")", limit = 2)
        if (indexAndKV.size != 2) {
          throw KoogCodegenError("Could not retrieve the index. Each line should be in the format `[0-9]+)old-column:new-column`. Got: $line")
        }
        val index = indexAndKV[0].trim().toIntOrNull()
          ?: throw KoogCodegenError("Index should be an integer. Got: ${indexAndKV[0]}")
        val oldAndNew = indexAndKV[1].split(":", limit = 2)
        if (oldAndNew.size != 2) {
          throw KoogCodegenError("Could not split the line into key-value pairs. Each line should be in the format `[0-9]+)old-column:new-column`. Got: $line")
        }
        val oldName = oldAndNew[0].trim()
        val newName = oldAndNew[1].trim()
        if (oldName.isEmpty() || newName.isEmpty()) {
          throw KoogCodegenError("Old and new names should not be empty. Got: oldName='$oldName', newName='$newName'")
        }
        if (index < 0) {
          throw KoogCodegenError("Index should be a non-negative integer. Got: $index")
        }
        Triple(index, oldName, newName)
      }

    val indexs = lines.map { it.first }
    // Verify that the indexes are contiguous and start from 1
    if (indexs != (1..indexs.size).toList()) {
      throw KoogCodegenError("Indexes should be contiguous and start from 1. Got: $indexs")
    }
    // check at every index to make sure the oldName is the same as the originalLabels[index - 1]
    val pairs = lines.map { (index, keyName, valueName) ->
      val originalName = originalLabels[index - 1]
      if (keyName != originalName) {
        log("[ExoQuery] WARNING: Old name '$valueName' does not match the original label ${originalName} (in $keyName:$originalName) at index $index. Using ${originalName} instead.")
      }
      originalName to valueName
    }
    return pairs
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
  private suspend fun executeAgentAndParse(requesterFactory: RequesterFactory, tableOrColumnNames: List<String>, maxNamesPerCall: Int, numParallel: Int, logDetails: Boolean, label: String): Map<String, String> = coroutineScope {
    val modelInputs = produceInputs(tableOrColumnNames, maxNamesPerCall)
    val chunkedInputs = modelInputs.chunked(numParallel)
    val modelName = requesterFactory.statedModelId
    val output =
      chunkedInputs.withIndex().flatMap { (i, batch) ->
        log("[ExoQuery] Processing batch ${i+1}/${chunkedInputs.size} with model ${modelName}")
        val batchOutputs = batch.withIndex().map { (j, request) ->
          async {
            val agent = requesterFactory.make()
            if (logDetails) log("[ExoQuery] =============== Agent Input ${i+1}-${j+1} (${label}): ===============\n${request.modelInput}")
            val output = agentCaller.call(agent, request)
            if (logDetails) log("[ExoQuery] =============== Agent Output ${i+1}-${j+1} (${label}): ===============\n${output}")
            request to output
          }
        }.awaitAll()
        // Parse the output of the agent as we go, if anything goes wrong immediately throw an error
        batchOutputs.map { (agentInput, agentOutputs) -> parseOutput(agentInput.originalTables, agentOutputs) }
      }.flatten().toMap()
    log("[ExoQuery] Completed processing model ${modelName}")
    output
  }

  class Live(log: (String) -> Unit): KoogBasedNameProcessor(log) {
    override val agentCaller: AgentCallerService = AgentCallerService.Live
  }
  class Test(val makeLlmOutput: (String) -> String, log: (String) -> Unit): KoogBasedNameProcessor(log) {
    override val agentCaller: AgentCallerService = AgentCallerService.Test(makeLlmOutput)
  }
}

interface AgentCallerService {
  suspend fun call(requester: Requester, input: ModelInput): String

  data object Live : AgentCallerService {
    override suspend fun call(requester: Requester, input: ModelInput): String =
      requester.execute(input.modelInput)
  }
  data class Test(val makeOutput: (String) -> String) : AgentCallerService {
    override suspend fun call(requester: Requester, input: ModelInput): String =
      makeOutput(input.modelInput)
  }
}


data class Requester(val request: suspend (String) -> List<Message.Response>) {
  suspend fun execute(userPrompt: String): String {
    val responses = request(userPrompt)
    return responses.map { it.content }.joinToString("\n")
  }
}

class RequesterFactory(val type: LLM, val systemPrompt: String, val rootLevelApiKey: String?) {

  public val statedModelId get() =
    when(type) {
      is LLM.Ollama -> type.model
      is LLM.OpenAI -> type.model
    }

  fun make(): Requester = run {
    when(type) {
      is LLM.Ollama -> {
        val clientModel = makeOllamaModel(type)
        Requester { userPrompt ->
          OllamaClient(type.url).execute(
            prompt = prompt(UUID.randomUUID().toString()) {
              system(systemPrompt)
              user(userPrompt)
            },
            model = clientModel
          )
        }
      }
      is LLM.OpenAI -> {
        val model = makeOpenAIModel(type)
        val apiKey = type.apiKey ?: rootLevelApiKey ?: throw KoogCodegenError(
          "OpenAI API key is not provided. Please specify it using TypeOfLLM.OpenAI.apiKey, TypeOfLLM.OpenAI.apiKeyEnvVar or the `api-key` field of your codegen config (.codegen.properties by default)."
        );
        Requester { userPrompt ->
          OpenAILLMClient(apiKey).execute(
            prompt = prompt(UUID.randomUUID().toString()) {
              system(systemPrompt)
              user(userPrompt)
            },
            model = model
          )
        }
      }
    }
  }

  private fun makeOllamaModel(ollamaType: LLM.Ollama) = run {
    val model = LLModel(
      provider = LLMProvider.Ollama,
      id = ollamaType.model,
      capabilities = listOf(
        LLMCapability.Temperature,
        LLMCapability.Schema.JSON.Simple
      )
    )
    model
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

  private fun makeOpenAIModel(openAiType: LLM.OpenAI) = run {
    val model =
      supportedOpenAiModels.find { it.id == openAiType.model }
        ?: throw KoogCodegenError("Unsupported OpenAI model: ${openAiType.model}. Supported models are: ${supportedOpenAiModels.joinToString(", ") { it.id }}")
    model
  }
}


fun NameParser.preparedForRuntime(log: (String) -> Unit = {}): NameParser =
  preparedFor(this, log, KoogBasedNameProcessor.Live(log))

fun NameParser.preparedForTesting(log: (String) -> Unit, makeLlmOutput: (String) -> String): NameParser =
  preparedFor(this, log, KoogBasedNameProcessor.Test(makeLlmOutput, log))

private fun preparedFor(nameParser: NameParser, log: (String) -> Unit = {}, koogProcessor: KoogBasedNameProcessor): NameParser {
  fun prepareRecurse(nameParser: NameParser): NameParser =
    when (nameParser) {
      is NameParser.UsingLLM -> nameParser.copy(processor = koogProcessor)
      is NameParser.Composite -> nameParser.copy(parsers = nameParser.parsers.map { prepareRecurse(it) })
      else -> nameParser // No need to change other parsers
    }
  return prepareRecurse(nameParser)
}

fun GeneratorBase<*, *>.preparedForRuntime(
  log: (String) -> Unit = {},
  agentCaller: AgentCallerService = AgentCallerService.Live
): JdbcGenerator =
  when (this) {
    is JdbcGenerator -> {
      val nameParser = this.config.nameParser.preparedForRuntime(log)
      this.withConfig(config = this.config.copy(nameParser = nameParser))
    }
    else -> throw IllegalArgumentException("GeneratorBase should be of type JdbcGenerator to prepare for runtime. Got: ${this::class.simpleName}")
  }
