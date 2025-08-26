package io.exoquery.generation

import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.GeneratorBase

actual fun Code.Entities.toGenerator(absoluteRootPath: String, projectBaseDir: String?, log: (String) -> Unit): GeneratorBase<*, *> {
  throw IllegalStateException("Code generation is not supported in this environment. Please use the appropriate code generation tool or library for your platform.")
}
actual fun Code.Entities.toLowLevelConfig(absoluteRootPath: String, propertiesBaseDir: String?): Pair<LowLevelCodeGeneratorConfig, PropsData> {
  throw IllegalStateException("Code generation (properties retrieval) is not supported in this environment. Please use the appropriate code generation tool or library for your platform.")
}
