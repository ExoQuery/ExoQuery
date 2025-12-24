package io.exoquery.codegen

import io.exoquery.codegen.model.CodeFile

data class CodeFileContent(val path: String, val packageDef: String, val codeDef: String) {
  companion object {

    /**
     * Extract the package def and the main body of the code from the following:
     * (ignore the imports as they are standard)
     *
     * ```
     * package foo.bar.schema
     *
     * import kotlinx.serialization.SerialName
     *
     * @SerialName("test_table")
     * data class test_table(val id: java.math.BigDecimal)
     * ```
     */
    fun parseCodeFile(code: String) = run {
      // get the package line
      val packageLine = code.trim().lines().firstOrNull { it.startsWith("package ") } ?: throw IllegalArgumentException("No package line found in the code")
      val packageDef = packageLine.removePrefix("package ").trim()
      val remainingCode = code.trim().lines().dropWhile { it != packageLine }.drop(1).joinToString("\n").trim()
      // strip out the import statements (i.e. the import block)
      val classCode = remainingCode.lines()
        .dropWhile { it.startsWith("import ") }
        .joinToString("\n")
        .trim()
      packageDef to classCode
    }



    fun equal(a: CodeFileContent, b: CodeFileContent): Boolean {
      return a.packageDef.trim() == b.packageDef.trim() && a.codeDef.trim() == b.codeDef.trim()
    }
  }
}

fun CodeFile.toContent(): CodeFileContent {
  val (packageDef, classCode) = CodeFileContent.parseCodeFile(this.code)
  return CodeFileContent(
    path = this.makeWritePath().toDirPath(),
    packageDef = packageDef,
    codeDef = classCode
  )
}
