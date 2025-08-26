package io.exoquery.generation

import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.GeneratorBase
import io.exoquery.generation.Code

actual fun Code.DataClasses.toGenerator(absoluteRootPath: String, projectBaseDir: String?, log: (String) -> Unit): GeneratorBase<*, *> {
  throw IllegalStateException("Code generation is not supported in this environment. Please use the appropriate code generation tool or library for your platform.")
}
actual fun Code.DataClasses.toLowLevelConfig(absoluteRootPath: String, propertiesBaseDir: String?): Pair<LowLevelCodeGeneratorConfig, PropsData> {
  throw IllegalStateException("Code generation (properties retrieval) is not supported in this environment. Please use the appropriate code generation tool or library for your platform.")
}
