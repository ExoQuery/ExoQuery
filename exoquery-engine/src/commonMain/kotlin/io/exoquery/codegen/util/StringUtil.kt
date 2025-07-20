package io.exoquery.codegen.util

fun String.indent(numIndents: Int): String {
  val code = this
  val lines = code.split("\n")
  val indentation = " ".repeat(numIndents)
  return lines.drop(1).joinToString("\n") { if (it.isEmpty()) it else indentation + "$it" }.let { lines.first() + "\n" + it }
}

fun String.snakeToUpperCamel(): String = split("_").joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }
fun String.snakeToLowerCamel(): String = snakeToUpperCamel().replaceFirstChar(Char::lowercase)
fun String.lowerCamelToSnake(): String = split("(?=[A-Z])".toRegex()).joinToString("_").lowercase()
fun String.uncapitalize(): String = replaceFirstChar(Char::lowercase)
fun String.capitalizeIt(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
// I.e. replace the first and last quote if either exist
fun String.unquote(): String = replaceFirst("^\"".toRegex(), "").replaceFirst("\"$".toRegex(), "")
fun String.trimFront(): String = dropWhile { it == '\n' }
fun String.notEmptyOrNull(): String? = if (trim().isEmpty()) null else this
fun String.inSetNocase(vararg seq: String): Boolean = seq.map(String::lowercase).contains(this.lowercase())


// Scala:
//object StringUtil {
//
//  def indent(code: String): String = {
//    val lines = code.split("\n")
//    lines.tail.foldLeft(lines.head) { (out, line) =>
//      out + '\n' +
//        (if (line.isEmpty) line else "  " + line)
//    }
//  }
//
//  implicit final class StringExtensions(private val str: String) extends AnyVal {
//    def snakeToUpperCamel: String = str.split("_").map(_.toLowerCase).map(_.capitalize).mkString
//    def snakeToLowerCamel: String = str.split("_").map(_.toLowerCase).map(_.capitalize).mkString.uncapitalize
//    def lowerCamelToSnake: String = str.split("(?=[A-Z])").mkString("_").toLowerCase
//    def uncapitalize: String =
//      new String(
//        (str.toList match {
//          case head :: tail => head.toLower :: tail
//          case Nil          => Nil
//        }).toArray
//      )
//    def unquote: String          = str.replaceFirst("^\"", "").replaceFirst("\"$", "")
//    def trimFront: String        = str.dropWhile(_ == '\n')
//    def notEmpty: Option[String] = if (str.trim == "") None else Some(str)
//    def inSetNocase(seq: String*): Boolean =
//      seq.map(_.toLowerCase).toSeq.contains(str.toLowerCase)
//  }
//
//  implicit final class OptionStringExtensions(private val str: Option[String]) extends AnyVal {
//    def existsInSetNocase(seq: String*): Boolean =
//      str.map(_.toLowerCase).exists(value => seq.map(_.toLowerCase).toSeq.contains(value))
//  }
//}
