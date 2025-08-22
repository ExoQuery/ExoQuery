package io.exoquery.plugin.transform

import io.exoquery.codegen.ai.preparedForRuntime
import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.model.CodeGenerationError
import io.exoquery.codegen.model.GeneratorBase
import io.exoquery.codegen.model.NameParser
import io.exoquery.config.ExoCompileOptions
import io.exoquery.generation.Code
import io.exoquery.generation.toGenerator
import io.exoquery.parseError
import io.exoquery.plugin.logging.Messages.AttemptingToUseLLMWhenDisabled
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name

class CodegenFileBuilder(val options: ExoCompileOptions) {
  context (CX.Scope)
  operator fun invoke(dcs: List<Code.DataClasses>, thisFile: IrFile) {
    dcs.forEach { dc ->

      // When the configuration uses an AI model, and we haven't enabled the AI model in the plugin options return an error
      // TODO double check this works as intended
      when (val aiConfig = dc.nameParser.findFirstConfigWithAI()) {
        NameParser.UsingLLM if (!options.enableCodegenAI) -> {
          parseError(AttemptingToUseLLMWhenDisabled(dc.driver.jdbcUrl, aiConfig))
        }
      }

      try {
        val rootPath = "${options.entitiesBaseDir}/${options.targetName}/${options.sourceSetName}/kotlin"
        val gen = dc.toGenerator(rootPath, options.projectDir).preparedForRuntime(
          {msg -> logger.warn(msg)}
        )
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
