package io.exoquery.generation

import io.exoquery.codegen.model.GeneratorBase

expect fun Code.DataClasses.toGenerator(absoluteRootPath: String): GeneratorBase<*, *, *>
