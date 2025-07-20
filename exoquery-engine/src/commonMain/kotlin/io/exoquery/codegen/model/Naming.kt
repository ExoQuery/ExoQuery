package io.exoquery.codegen.model

import io.exoquery.codegen.util.KotlinLangUtil
import io.exoquery.codegen.util.indent

sealed interface FileNamingStrategy

/**
 * Name each package by the name of the table being generated. If multiple
 * tables are going to the generator, need to choose which one to use, most
 * likely the 1st. Typically used in ByPackage strategies. This is the most
 * common use-case.
 */
data object ByTable : FileNamingStrategy

/**
 * Typically used when multiple Tables are grouped into the same schema. but a
 * package object is not used.
 */
data object BySchema : FileNamingStrategy


interface DataClassNaming {
    val tableColumns: TablePrepared
    val rawCaseClassName: String get() = tableColumns.name
    val actualCaseClassName: String get() = KotlinLangUtil.escape(rawCaseClassName)
}

interface FieldNaming {
    val column: ColumnPrepared
    val rawFieldName: String get() = column.name
    val fieldName: String get() = KotlinLangUtil.escape(rawFieldName)
}
