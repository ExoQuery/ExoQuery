package io.exoquery

import io.exoquery.printing.PrintableValue

object MessageFileKotlinMaker {
  private fun String.indentBy(spaces: Int) = this.lines().joinToString("\n") { " ".repeat(spaces) + it }
  private fun String.isMultiline() = this.contains('\n') || this.contains('\r')

  private fun PrintableValue.renderParamsLine() =
    if (params.isEmpty()) ""
    else ",\n      " + params.map { "\"${it.id}\" to \"${it.value}\"" }.joinToString(", ")

  private fun singleLineQuery(label: String, query: String, printable: PrintableValue) = run {
    val type = printable.type
    val paramsLine = printable.renderParamsLine()

    val qqq = """"""".repeat(3)
    val row =
      if (query.contains('"')) {
        """|    "${label}" to ${type.interpolatorPrefix}(
           |      ${qqq}${query}${qqq}${paramsLine}
           |    ),
        """.trimMargin()
      } else {
        """|    "${label}" to ${type.interpolatorPrefix}(
           |      "${query}"${paramsLine}
           |    ),
        """.trimMargin()
      }

    row
  }

  private fun multiLineQuery(label: String, query: String, printable: PrintableValue) = run {
    val type = printable.type
    val paramsLine = printable.renderParamsLine()

    // create every line of the GoldeQueryFile
    val qqq = """"""".repeat(3)
    val row =
      """|    "${label}" to ${type.interpolatorPrefix}(
         |      ${qqq}
         |${query.indentBy(6)}
         |      ${qqq}${paramsLine}
         |    ),
      """.trimMargin()
    row
  }

  operator fun invoke(queries: List<PrintableValue>, fileName: String, filePackage: String): String {

    val dol = '$'
    fun String.escapeDollar() = this.replace("$", "${dol}{'${dol}'}")

    val fileBody =
      queries
        .filter { qf -> qf.label != null }
        .mapNotNull { qf ->
          val label = qf.label!!
          if (qf.value.isMultiline())
            multiLineQuery(label, qf.value.escapeDollar(), qf)
          else
            singleLineQuery(label, qf.value.escapeDollar(), qf)
        }.joinToString("\n")

    return (
        """|package $filePackage
         |
         |import io.exoquery.printing.GoldenResult
         |import io.exoquery.printing.cr
         |import io.exoquery.printing.kt
         |import io.exoquery.printing.pl
         |
         |object ${fileName}: MessageSpecFile {
         |  override val messages = mapOf<String, GoldenResult>(
         |$fileBody
         |  )
         |}
         |""".trimMargin()
        )
  }
}
