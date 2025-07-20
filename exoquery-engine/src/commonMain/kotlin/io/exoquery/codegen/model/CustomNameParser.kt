package io.exoquery.codegen.model

import io.exoquery.codegen.util.*

sealed interface NameParser {
  fun parseColumn(cm: ColumnMeta): String
  fun parseTable(tm: TableMeta): String

  object LiteralNames : NameParser {
    override fun parseColumn(cm: ColumnMeta): String = cm.columnName
    override fun parseTable(tm: TableMeta): String = tm.tableName.capitalizeIt()
  }

  object SnakeCaseNames : NameParser {
    override fun parseColumn(cm: ColumnMeta): String = cm.columnName.snakeToLowerCamel()
    override fun parseTable(tm: TableMeta): String = tm.tableName.snakeToUpperCamel()
  }

  data class CustomNames(
    private val columnParser: (ColumnMeta) -> String = { it.columnName.snakeToLowerCamel() },
    private val tableParser: (TableMeta) -> String = { it.tableName.snakeToUpperCamel() }
  ) : NameParser {
    override fun parseColumn(cm: ColumnMeta): String = columnParser(cm)
    override fun parseTable(tm: TableMeta): String = tableParser(tm)
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
