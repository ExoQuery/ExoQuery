package io.exoquery.codegen.gen

abstract class AbstractCodeEmitter {
  abstract val code: String

  inner abstract class AbstractCaseClassGen {
    abstract val code: String
    abstract val rawCaseClassName: String
    abstract val actualCaseClassName: String

    inner abstract class AbstractMemberGen {
      open val code: String get() = "$fieldName: $actualType"
      abstract val rawType: String
      abstract val actualType: String
      abstract val rawFieldName: String
      abstract val fieldName: String
    }
  }
}
