package io.exoquery.codegen.gen

import io.exoquery.codegen.model.ColumnPrepared
import io.exoquery.codegen.model.NamingAnnotationType
import io.exoquery.codegen.model.TablePrepared
import io.exoquery.codegen.util.KotlinLangUtil
import io.exoquery.codegen.util.indent

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
        if (caseClassName == databaseName && caseClassName.none { it.isUpperCase() }) {
          "@Serializable\n$dataClassCode"
        } else {
          "@Serializable\n$annotation\n$dataClassCode"
        }

    inner class MemberGen(val column: ColumnPrepared): AbstractMemberGen() {
      override val rawType: String = column.dataType.value
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

      /**
       * Do not add the annotation if the field name matches the database name and is all lowercase
       * (if there are uppercase letters, it may be a camelCase name in the DB,
       * e.g. we would do `SELECT person.orgName` instead of `SELECT person."orgName"` which would fail
       * so we still need the annotation even if the data-class field name is `orgName`)
       */
      override val code: String
        get() = if (fieldName == databaseName && fieldName.none { it.isUpperCase() }) {
          "val $fieldName: $actualType"
        } else {
          "$annotation val $fieldName: $actualType"
        }
    }
  }
}
