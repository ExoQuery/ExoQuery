package io.exoquery.generation

import io.exoquery.codegen.model.GeneratorBase
import io.exoquery.generation.Code

actual fun Code.DataClasses.toGenerator(absoluteRootPath: String): GeneratorBase<*, *, *> {
  throw IllegalStateException("Code generation is not supported in this environment. Please use the appropriate code generation tool or library for your platform.")
}
