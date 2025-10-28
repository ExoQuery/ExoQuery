package io.exoquery.plugin.settings

import io.exoquery.config.ExoCompileOptions
import org.jetbrains.kotlin.config.CompilerConfigurationKey

val EXO_OPTIONS = CompilerConfigurationKey.create<ExoCompileOptionsBuilder>("ExoQuery options")

// TODO cleanup. Should they all be null initially?
class ExoCompileOptionsBuilder {
  var entitiesBaseDir: String? = null
  var generationDir: String? = null
  var projectSrcDir: String? = null
  var sourceSetName: String? = null
  var targetName: String? = null
  var parentSourceSetNames: List<String> = emptyList()
  var queriesBaseDir: String? = null
  var storedBaseDir: String? = null
  var projectDir: String? = null
  var outputString: String? = null
  var queryFilesEnabled: Boolean? = true
  var queryPrintingEnabled: Boolean? = true
  var enableCodegenAI: Boolean? = false
  var forceRegen: Boolean? = false
  var enableCrossFileStore: Boolean? = ExoCompileOptions.EnableCrossFileStore


  fun build(): ExoCompileOptions {
    return ExoCompileOptions(
      requireNotNull(entitiesBaseDir) { "A non-null base-directory for generated entities (i.e. from the sql.generate block" },
      requireNotNull(generationDir) { "A non-null generationDir must be provided" },
      requireNotNull(projectSrcDir) { "A non-null projectSrcDir must be provided" },
      requireNotNull(sourceSetName) { "A non-null sourceSetName must be provided" },
      requireNotNull(targetName) { "A non-null targetName (of the kotlin-compilation) must be provided" },
      requireNotNull(parentSourceSetNames) { "A non-null list of parent source set names must be provided" },
      requireNotNull(queriesBaseDir) { "A non-null base directory for sql-entities must be provided" },
      requireNotNull(storedBaseDir) { "A non-null base directory for StoredXRs.db files must be provided" },
      requireNotNull(projectDir) { "A non-null projectDir must be provided" },
      outputString ?: ExoCompileOptions.DefaultOutputString,
      queryFilesEnabled ?: ExoCompileOptions.DefaultQueryFilesEnabled,
      queryPrintingEnabled ?: ExoCompileOptions.DefaultQueryPrintingEnabled,
      enableCodegenAI ?: ExoCompileOptions.DefaultEnabledCodegenAI,
      forceRegen ?: ExoCompileOptions.DefaultForceRegen,
      enableCrossFileStore ?: ExoCompileOptions.EnableCrossFileStore,
    )
  }
}



fun ExoCompileOptionsBuilder.processOption(option: ExoCliOption, value: String) = when (option) {
  ExoCliOption.ENTITES_DIR_OPTION -> entitiesBaseDir = value
  ExoCliOption.GENERATION_DIR_OPTION -> generationDir = value
  ExoCliOption.PROJECT_SRC_DIR_OPTION -> projectSrcDir = value
  ExoCliOption.SOURCE_SET_NAME_OPTION -> sourceSetName = value
  ExoCliOption.TARGET_NAME_OPTION -> targetName = value
  ExoCliOption.PARENT_SOURCE_SET_NAMES_OPTION -> parentSourceSetNames =
    value.split(";").map { it.trim() }.filter { it.isNotEmpty() }
  ExoCliOption.QUERIES_BASE_DIR_OPTION -> queriesBaseDir = value
  ExoCliOption.STORED_BASE_DIR_OPTION -> storedBaseDir = value
  ExoCliOption.PROJECT_DIR_KEY -> projectDir = value
  ExoCliOption.OUTPUT_STRING -> outputString = value
  ExoCliOption.QUERY_FILES_ENABLED -> queryFilesEnabled = value.toBoolean()
  ExoCliOption.QUERY_PRINTING_ENABLED -> queryPrintingEnabled = value.toBoolean()
  ExoCliOption.ENABLE_CODEGEN_AI -> enableCodegenAI = value.toBoolean()
  ExoCliOption.FORCE_REGEN -> forceRegen = value.toBoolean()
  ExoCliOption.ENABLE_CROSS_FILE_STORE -> enableCrossFileStore = value.toBoolean()
}
