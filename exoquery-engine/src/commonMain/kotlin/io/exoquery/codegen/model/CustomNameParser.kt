package io.exoquery.codegen.model

import io.exoquery.codegen.util.*
import kotlinx.serialization.Serializable as Ser


interface NameProcessorLLM {
  fun processTables(usingLLM: NameParser.UsingLLM, tables: List<TablePrepared>): List<TablePrepared>

  data class ModelInput(val originalTables: List<String>, val modelInput: String)

  @Ser
  object CompileTimeProvided: NameProcessorLLM {
    override fun processTables(usingLLM: NameParser.UsingLLM, tables: List<TablePrepared>): List<TablePrepared> =
      throw IllegalArgumentException("This is a placeholder that tells ExoQuery to use the compile-time-provided name processor. Leave the default in this field.")
  }
  companion object {
  }
}

@Ser
sealed interface NameParser {
  fun parseTables(tables: List<TablePrepared>): List<TablePrepared>

  sealed interface TypeOfLLM {
    data class Ollama(
      val model: String = DefaultModel,
      val url: String = DefaultUrl
    ) : TypeOfLLM {
      companion object {
        val DefaultModel = "qwen2.5-coder:0.5b"
        val DefaultUrl = "http://localhost:11434"
      }
    }
    data class OpenAI(
      val model: String = DefaultModel,
      /**
       * The API key to use for OpenAI requests.
       * DO NOT USE THIS IN PRODUCTION. Instead use `apiKeyEnvVar` to set the API key as an environment variable
       * or use the add `api-key` to your codge-generation properties file (.codegen.properties by default).
       */
      val apiKey: String? = null,
      val apiKeyEnvVar: String? = null,
    ): TypeOfLLM {
      companion object {
        val DefaultModel = "gpt-4o-mini"
      }
    }
    companion object {
    }
  }
  @Ser
  data class UsingLLM(
    val type: TypeOfLLM,
    val maxTablesPerCall: Int = DefaultMaxTablesPerCall,
    val maxColumnsPerCall: Int = DefaultMaxColumnsPerCall,
    val systemPromptTables: String = DefaultSystemPromptTables,
    val systemPromptColumns: String = DefaultSystemPromptColumns,
    val processor: NameProcessorLLM = NameProcessorLLM.CompileTimeProvided
  ): NameParser {
    override fun parseTables(tables: List<TablePrepared>): List<TablePrepared> =
      processor.processTables(this, tables)

    companion object {
      val DefaultMaxTablesPerCall = 20
      val DefaultMaxColumnsPerCall = 20
      val DefaultSystemPromptTables = """ 
        Convert a list of labels to UpperCamelCase names and return a list of old-label:new-label.
        Find what english words make sense to convert to upper case based on their semantic meaning.
        
        Example Input:
        <INPUT>
        1)Foobarbaz
        2)one_Two_three
        3)Carentity
        4)youngperson
        5)OldPerson
        6)original_sales_record
        </INPUT>
        
        Example Output: 
        <OUTPUT>
        1)Foobarbaz:FooBarBaz
        2)one_Two_three:OneTwoThree
        3)Carentity:CarEntity
        4)youngperson:YoungPerson
        5)OldPerson:OldPerson
        6)original_sales_record:OriginalSalesRecord
        </OUTPUT>
      """.trimIndent()

      val DefaultSystemPromptColumns = """ 
        Convert a list of labels to lowerCamelCase names and return a list of old-label:new-label.
        Find what english words make sense to convert to upper case based on their semantic meaning.
        
        Example Input (old-label):
        <INPUT>
        1)Foobarbaz
        2)one_Two_three
        3)Carentity
        4)youngperson
        5)OldPerson
        6)original_sales_record
        </INPUT>
        
        Example Output (old-label:new-label):
        <OUTPUT>
        1)Foobarbaz:fooBarBaz
        2)one_Two_three:oneTwoThree
        3)Carentity:carEntity
        4)youngperson:youngPerson
        5)OldPerson:oldPerson
        6)original_sales_record:originalSalesRecord
        </OUTPUT>
      """.trimIndent()
    }
  }

  @Ser sealed interface SimpleNameParser : NameParser {
    override fun parseTables(tables: List<TablePrepared>): List<TablePrepared> =
      tables.map { t ->
        t.copy(
          name = parseTable(t),
          columns = t.columns.map { c ->
            c.copy(name = parseColumn(c))
          }
        )
      }

    fun parseColumn(cm: ColumnPrepared): String
    fun parseTable(tm: TablePrepared): String
  }

  @Ser object LiteralNames : SimpleNameParser {
    override fun parseColumn(cm: ColumnPrepared): String = cm.name
    override fun parseTable(tm: TablePrepared): String = tm.name.capitalizeIt()
  }

  @Ser object SnakeCaseNames : SimpleNameParser {
    override fun parseColumn(cm: ColumnPrepared): String = cm.name.snakeToLowerCamel()
    override fun parseTable(tm: TablePrepared): String = tm.name.snakeToUpperCamel()
  }

  companion object {
  }
}
