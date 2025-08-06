package io.exoquery.plugin.transform

import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.GeneratorBase
import io.exoquery.config.ExoCompileOptions
import io.exoquery.generation.Code
import io.exoquery.generation.toGenerator
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name

class CodegenFileBuilder(val options: ExoCompileOptions) {
  context (CX.Scope)
  operator fun invoke(dcs: List<Code.DataClasses>, thisFile: IrFile) {
    dcs.forEach { dc ->
      try {
        val rootPath = "${options.entitiesBaseDir}/${options.targetName}/${options.sourceSetName}/kotlin"
        val gen = dc.toGenerator(rootPath)
        logger.warn("Generating Code for ${thisFile.name} in: ${rootPath}")
        gen.run()
      } catch (t: Throwable) {
         logger.error(
           "Code Generation Failed for the database ${dc.driver.jdbcUrl}\n================== Cause ==================\n${t.stackTraceToString()}"
         )
      }
    }
  }
}
