package io.exoquery.codegen.model

import kotlin.reflect.KClass

data class TablePrepared(
  val namespace: String,
  val name: String,
  val columns: List<ColumnPrepared>,
  val meta: TableMeta
)

data class ColumnPrepared(
  val name: String,
  val dataType: ClassName,
  val nullable: Boolean,
  val meta: ColumnMeta
)

sealed interface NamingAnnotationType {
  data object ExoField : NamingAnnotationType
  data object SerialName : NamingAnnotationType
}
