package io.exoquery.printing

import io.exoquery.terpal.parseError
import io.exoquery.xr.CollectXR
import io.exoquery.xr.XR
import io.exoquery.xrError

fun <T> List<T>.singleIs(predicate: (T) -> Boolean): Boolean =
  this.size == 1 && this.singleOrNull()?.let(predicate) ?: false

fun <T> List<T>.doubleIs(predicate: (T, T) -> Boolean): Boolean =
  this.size == 2 && predicate(this[0], this[1])

object QueryFileKotlinMakerRoom {
  private fun String.indentBy(spaces: Int) = this.lines().joinToString("\n") { " ".repeat(spaces) + it }
  private fun String.commentOut() = this.lines().joinToString("\n") { "// ${it}" }
  private fun String.isMultiline() = this.contains('\n') || this.contains('\r')

  // TODO also get the package name of the current file and use that to shorten the class names where possible
  fun XR.ClassId.shortenIfPossible(): XR.ClassId =
    if (this.packageFqName.path.singleIs { it == "kotlin" } || this.packageFqName.path.doubleIs { a, b -> a == "kotlin" && (b == "collections" || b == "annotation" || b == "comparisons" || b == "io" || b == "ranges" || b == "sequences" || b == "text" ) } )
      copy(packageFqName = XR.FqName.Empty)
    else
      this

  fun XR.ClassId.show(): String =
    (this.packageFqName.path + this.relativeClassName.path).joinToString(".") +
      (if (this.typeArgs.isEmpty()) "" else this.typeArgs.joinToString(prefix = "<", postfix = ">") { it.render() })

  fun XR.ClassId.render() =
    this.shortenIfPossible().show()


  // TODO it's probably more reliable to get the variable data from the Token value but we
  //      need to introduce a new kind of `RoomParam` Token for that
  fun PrintableValue.toVariablesLine(): String =
    (when (val printableType = type) {
      is PrintableValue.Type.SqlQuery -> {
        //CollectXR.byType<XR.PlaceholderParam>(printableType.xr)
        val collected = CollectXR(printableType.xr) {
          when (it) {
            is XR.PlaceholderParam -> it
            else -> null
          }
        }

        collected
      }
      // TODO need some kind of warning here because this type should not be used to create room query files
      else -> {
        emptyList()
      }
    }).map { "${it.name}: ${it.originalType.render()}" }.joinToString(", ")

  private fun singleLineQuery(label: String, query: String, printable: PrintableValue) = run {
    val variablesLine = printable.toVariablesLine()
    // create every line of the GoldeQueryFile
    val type = printable.type

    val qqq = """"""".repeat(3)
    val row = // TODO if it's `val x = sql.select { ... }` then `x` should be the default label
      if (query.contains('"')) {
        """|  @Query(${qqq}${query}${qqq})
           |  fun ${label}(${variablesLine}): ${printable.queryOutputType.render()}
        """.trimMargin()
      } else {
        """|  @Query("${query}")
           |  fun ${label}(${variablesLine}): ${printable.queryOutputType.render()}
        """.trimMargin()
      }

    row
  }

  private fun multiLineQuery(label: String, query: String, printable: PrintableValue) = run {
    val type = printable.type
    val variablesLine = printable.toVariablesLine()

    val qqq = """"""".repeat(3)
    val row =
      """|  @Query(
         |    ${qqq}
         |${query.indentBy(4)}
         |    ${qqq}
         |  )
         |  fun ${label}(${variablesLine}): ${printable.queryOutputType.render()}
      """.trimMargin()
    row
  }

  operator fun invoke(queries: List<PrintableValue>, fileName: String, filePackage: String): String {
    val dol = '$'
    fun String.escapeDollar() = this.replace("$", "$dol{'$dol'}")

    val fileBody =
      queries
        .map { qf ->
          if (qf.label == null) {
            // TODO when printable-value includes locaiton string add here
            ("// Query without label:\n" + qf.value.commentOut()).indentBy(2) + "\n"
          } else if (qf.value.isMultiline())
            multiLineQuery(qf.label, qf.value.escapeDollar(), qf)
          else
            singleLineQuery(qf.label, qf.value.escapeDollar(), qf)
        }.joinToString("\n")

    // Need to have 'mapOf<String, String>' not just mapOf because otherwise when it is empty the type won't be inferred leading to a compile error
    return (
        """|package $filePackage
         |
         |import androidx.room.Dao
         |import androidx.room.Query
         |
         |@Dao
         |interface ${fileName} {
         |$fileBody
         |}
         |""".trimMargin()
        )
  }
}
