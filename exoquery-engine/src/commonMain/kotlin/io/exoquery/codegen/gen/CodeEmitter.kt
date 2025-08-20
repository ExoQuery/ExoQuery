package io.exoquery.codegen.gen

import io.exoquery.codegen.model.ColumnPrepared
import io.exoquery.codegen.model.NamingAnnotationType
import io.exoquery.codegen.model.TablePrepared
import io.exoquery.codegen.util.KotlinLangUtil
import io.exoquery.codegen.util.indent

// Kotlin:
class CodeEmitter(
  val emitterDeliverable: CodeEmitterDeliverable
): AbstractCodeEmitter() {
  val caseClassTables: List<TablePrepared> = emitterDeliverable.tables

  val caseClassesCode: String get() = caseClassTables.joinToString("\n\n") { CaseClassGen(it).code }

  override val code: String
    get() = emitterDeliverable.codeWrapper.surround(
      caseClassesCode
    )


  inner class CaseClassGen(val table: TablePrepared): AbstractCaseClassGen() {
    override val rawCaseClassName: String    = table.name
    override val caseClassName: String = KotlinLangUtil.escape(rawCaseClassName)
    val databaseName = table.meta.tableName

    private val tableColumnsCode: String get() =
      if (table.columns.size <= 3) {
        table.columns.joinToString(", ") { MemberGen(it).code }
      } else {
        "\n" + "  " + table.columns.joinToString(",\n") { MemberGen(it).code }.indent(2) + "\n"
      }

    private val annotation get() =
      when (emitterDeliverable.tableNamingAnnotationType) {
        is NamingAnnotationType.ExoField -> "@ExoEntity(\"$databaseName\")"
        is NamingAnnotationType.SerialName -> "@SerialName(\"$databaseName\")"
      }

    private val dataClassCode get() = "data class $caseClassName(" + tableColumnsCode + ")"

    override val code: String
      get() =
        if (caseClassName != databaseName) {
          "$annotation\n$dataClassCode"
        } else {
          dataClassCode
        }

    inner class MemberGen(val column: ColumnPrepared): AbstractMemberGen() {
      override val rawType: String = column.dataType.value ?: "String"
      override val actualType: String =
        KotlinLangUtil.escape(rawType.replaceFirst(Regex("^kotlin\\."), "")) + run {
          if (column.nullable) {
            "?"
          } else {
            ""
          }
        }
      override val rawFieldName: String = column.name
      override val fieldName: String = KotlinLangUtil.escape(rawFieldName)
      val databaseName = column.meta.columnName

      private val annotation get() =
        when (emitterDeliverable.columnNamingAnnotationType) {
          is NamingAnnotationType.ExoField -> "@ExoField(\"$databaseName\")"
          is NamingAnnotationType.SerialName -> "@SerialName(\"$databaseName\")"
        }

      override val code: String
        get() = if (fieldName != databaseName) {
          "$annotation val $fieldName: $actualType"
        } else {
          "val $fieldName: $actualType"
        }
    }
  }
}
