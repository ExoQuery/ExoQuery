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


  inner class CaseClassGen(val tableColumns: TablePrepared): AbstractCaseClassGen() {
    override val rawCaseClassName: String    = tableColumns.name
    override val actualCaseClassName: String = KotlinLangUtil.escape(rawCaseClassName)

    private val tableColumnsCode: String get() =
      if (tableColumns.columns.size <= 3) {
        tableColumns.columns.joinToString(", ") { MemberGen(it).code }
      } else {
        "\n" + "  " + tableColumns.columns.joinToString(",\n") { MemberGen(it).code }.indent(2) + "\n"
      }

    override val code: String
      get() = "data class $actualCaseClassName(" + tableColumnsCode + ")"

    inner class MemberGen(val column: ColumnPrepared): AbstractMemberGen() {
      override val rawType: String = column.dataType.qualifiedName ?: column.dataType.simpleName ?: "String"
      override val actualType: String = KotlinLangUtil.escape(rawType.replaceFirst(Regex("^kotlin\\."), ""))
      override val rawFieldName: String = column.meta.columnName
      override val fieldName: String = KotlinLangUtil.escape(rawFieldName)

      private val annotation get() =
        when (emitterDeliverable.columnNamingAnnotationType) {
          is NamingAnnotationType.ExoField -> "@ExoField(\"$rawFieldName\")"
          is NamingAnnotationType.SerialName -> "@SerialName(\"$rawFieldName\")"
        }

      override val code: String
        get() = if (fieldName != rawFieldName) {
          "$annotation val $fieldName: $actualType"
        } else {
          "val $fieldName: $actualType"
        }
    }
  }
}


// Scala:
//  class CodeEmitter(emitterSettings: EmitterSettings)
//      extends AbstractCodeEmitter
//      with PackageGen {
//    import io.getquill.codegen.util.ScalaLangUtil._
//
//    val caseClassTables: Seq[TablePrepared] = emitterSettings.caseClassTables
//
//    override def code = surroundByPackage(body)
//    def body: String  = caseClassesCode + "\n\n" + tableSchemasCode
//
//    def caseClassesCode: String  = caseClassTables.map(CaseClass(_).code).mkString("\n\n")
//
//    protected def ifMembers(str: String) = if (renderMembers) str else ""
//
//    def CaseClass = new CaseClassGen(_)
//    class CaseClassGen(val tableColumns: TablePrepared)
//        extends super.AbstractCaseClassGen
//        with CaseClassNaming {
//      def code =
//        s"case class ${actualCaseClassName}(" + tableColumns.columns.map(Member(_).code).mkString(", ") + ")"
//
//      def Member = new MemberGen(_)
//      class MemberGen(val column: ColumnFusion[ColumnMeta])
//          extends super.AbstractMemberGen
//          with FieldNaming[ColumnMeta] {
//        override def rawType: String = column.dataType.toString()
//        override def actualType: String = {
//          val tpe = escape(rawType).replaceFirst("java\\.lang\\.", "")
//          if (column.nullable) s"Option[${tpe}]" else tpe
//        }
//      }
//    }
//  }
