package io.exoquery.generation

import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.GeneratorBase

data class PropsData(val user: String?, val password: String?, val apiKey: String?)

expect fun Code.DataClasses.toLowLevelConfig(absoluteRootPath: String, propertiesBaseDir: String?): Pair<LowLevelCodeGeneratorConfig, PropsData>
expect fun Code.DataClasses.toGenerator(absoluteRootPath: String, projectBaseDir: String? = null): GeneratorBase<*, *>
