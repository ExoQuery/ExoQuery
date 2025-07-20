package io.exoquery.codegen.util

object KotlinLangUtil {
  fun escape(str: String): String =
    if (isKeyword(str)) "`$str`" else str

  fun isKeyword(word: String): Boolean =
    keywords.contains(word.trim())

  private val keywords = setOf(
    "abstract", "as", "break", "class", "continue", "do", "else",
    "false", "final", "for", "fun", "if", "in", "interface",
    "is", "null", "object", "package", "return", "super",
    "this", "throw", "true", "try", "typealias", "val",
    "var", "when", "while"
  )
}

//object ScalaLangUtil {
//  def escape(str: String) =
//    if (isKeyword(str)) s"`${str}`" else str
//
//  def isKeyword(word: String) = keywords.contains(word.trim)
//  private val keywords = Set(
//    "abstract",
//    "case",
//    "catch",
//    "class",
//    "def",
//    "do",
//    "else",
//    "extends",
//    "false",
//    "final",
//    "finally",
//    "for",
//    "forSome",
//    "if",
//    "implicit",
//    "import",
//    "lazy",
//    "match",
//    "new",
//    "null",
//    "object",
//    "override",
//    "package",
//    "private",
//    "protected",
//    "return",
//    "sealed",
//    "super",
//    "this",
//    "throw",
//    "trait",
//    "try",
//    "true",
//    "type",
//    "val",
//    "var",
//    "while",
//    "with",
//    "yield"
//  )
//}
