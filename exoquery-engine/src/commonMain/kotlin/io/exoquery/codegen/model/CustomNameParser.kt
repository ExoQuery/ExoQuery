package io.exoquery.codegen.model

import io.exoquery.codegen.util.*

interface NameProcessorLLM {
  suspend fun processTables(usingLLM: NameParser.UsingLLM, tables: List<TablePrepared>): List<TablePrepared>

  object CompileTimeProvided: NameProcessorLLM {
    override suspend fun processTables(usingLLM: NameParser.UsingLLM, tables: List<TablePrepared>): List<TablePrepared> =
      throw IllegalArgumentException("This is a placeholder that tells ExoQuery to use the compile-time-provided name processor. Leave the default in this field.")
  }
  companion object {
  }
}

sealed interface NameParser {
  fun parseTables(tables: List<TablePrepared>): List<TablePrepared>

  sealed interface TypeOfLLM {
    data class Ollama(
      val model: String = DefaultModel,
      val url: String = DefaultUrl
    ) : TypeOfLLM {
      companion object {
        val DefaultModel = "qwen3:0.6b"
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
  data class UsingLLM(
    val type: TypeOfLLM,
    val maxTablesPerCall: Int = DefaultMaxTablesPerCall,
    val maxColumnsPerCall: Int = DefaultMaxColumnsPerCall,
    val systemPrompt: String = DefaultSystemPrompt,
    val processor: NameProcessorLLM = NameProcessorLLM.CompileTimeProvided
  ): NameParser {
    override fun parseTables(tables: List<TablePrepared>): List<TablePrepared> =
      processor.processTables(this, tables)

    companion object {
      val DefaultMaxTablesPerCall = 20
      val DefaultMaxColumnsPerCall = 20
      val DefaultSystemPrompt = """ 
        Convert a list of labels to UpperCamelCase names and return a list of old-label:new-label.
        Find what english words make sense to convert to upper case based on their semantic meaning.
        
        Example Input:
        <INPUT>
        Foobarbaz
        Carentity
        youngperson
        OldPerson
        </INPUT>
        
        Example Output: 
        <OUTPUT>
        Foobarbaz:FooBarbaz
        Carentity:CarEntity
        youngperson:YoungPerson
        OldPerson:OldPerson
        </OUTPUT>
      """.trimIndent()
    }
  }

  sealed interface SimpleNameParser : NameParser {
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

  object LiteralNames : SimpleNameParser {
    override fun parseColumn(cm: ColumnPrepared): String = cm.name
    override fun parseTable(tm: TablePrepared): String = tm.name.capitalizeIt()
  }

  object SnakeCaseNames : SimpleNameParser {
    override fun parseColumn(cm: ColumnPrepared): String = cm.name.snakeToLowerCamel()
    override fun parseTable(tm: TablePrepared): String = tm.name.snakeToUpperCamel()
  }

  companion object {
  }
}

// Scala:
//sealed trait NameParser {
//  def generateQuerySchemas: Boolean
//      def parseColumn(cm: JdbcColumnMeta): String
//  def parseTable(tm: JdbcTableMeta): String
//}
//
//trait LiteralNames extends NameParser {
//  def generateQuerySchemas                    = false
//  def parseColumn(cm: JdbcColumnMeta): String = cm.columnName
//  def parseTable(tm: JdbcTableMeta): String   = tm.tableName.capitalize
//}
//trait SnakeCaseNames extends NameParser {
//  def generateQuerySchemas                    = false
//  def parseColumn(cm: JdbcColumnMeta): String = cm.columnName.snakeToLowerCamel
//  def parseTable(tm: JdbcTableMeta): String   = tm.tableName.snakeToUpperCamel
//}
//
//object LiteralNames   extends LiteralNames
//object SnakeCaseNames extends SnakeCaseNames
//
//case class CustomNames(
//  columnParser: JdbcColumnMeta => String = cm => cm.columnName.snakeToLowerCamel,
//tableParser: JdbcTableMeta => String = tm => tm.tableName.snakeToUpperCamel
//) extends NameParser {
//  def generateQuerySchemas                    = true
//  def parseColumn(cm: JdbcColumnMeta): String = columnParser(cm)
//  def parseTable(tm: JdbcTableMeta): String   = tableParser(tm)
//}
//
