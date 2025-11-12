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
