package io.exoquery.plugin.settings

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption

enum class ExoCliOption(
  override val optionName: String,
  override val valueDescription: String,
  override val description: String,
  override val required: Boolean = false,
  override val allowMultipleOccurrences: Boolean = false
) : AbstractCliOption {
  ENTITES_DIR_OPTION(
    "entitiesBaseDir",
    "<entitiesBaseDir>",
    "Base directory for generated entities",
    false
  ),

  GENERATION_DIR_OPTION(
    "generationDir",
    "<generationDir>",
    "Dir of generated sources",
    false
  ),

  PROJECT_SRC_DIR_OPTION(
    "projectSrcDir",
    "<projectSrcDir>",
    "Dir of project sources",
    false
  ),

  SOURCE_SET_NAME_OPTION(
    "sourceSetName",
    "<sourceSetName>",
    "Name of the source set",
    false
  ),

  TARGET_NAME_OPTION(
    "targetName",
    "<targetName>",
    "Name of the kotlin compilation target",
    false
  ),

  PARENT_SOURCE_SET_NAMES_OPTION(
    "parentSourceSetNames",
    "<parentSourceSetNames>",
    "Comma-separated names of parent source sets",
    false
  ),

  QUERIES_BASE_DIR_OPTION(
    "queriesBaseDir",
    "<queriesBaseDir>",
    "path to sql queries base directory",
    false
  ),

  STORED_BASE_DIR_OPTION(
    "storedBaseDir",
    "<storedBaseDir>",
    "root path to StoredXRs.db files tree (containing subdirectories for each compiler target)",
    false
  ),

  PROJECT_DIR_KEY(
    "projectDir",
    "<projectDir>",
    "path to gradle project",
    false
  ),

  OUTPUT_STRING(
    "outputString",
    "<outputString>",
    "override the string printed when a static query is executed use %total and %sql switches to specify",
    false
  ),

  QUERY_FILES_ENABLED(
    "queryFilesEnabled",
    "<true|false>",
    "Enables/disables the generation of exo/generated/_.queries.sql query files",
    false
  ),

  QUERY_PRINTING_ENABLED(
    "queryPrintingEnabled",
    "<true|false>",
    "Enables/disables the printing of queries during compile-time",
    false
  ),

  ENABLE_CODEGEN_AI(
    "enableCodegenAI",
    "<true|false>",
    "Enables/disables the use of AI models for code generation",
    false
  ),

  FORCE_REGEN(
    "forceRegen",
    "<true|false>",
    "Forces regeneration of all entities, even if they are up-to-date",
    false
  ),

  ENABLE_CROSS_FILE_STORE(
    "enableCrossFileStore",
    "<true|false>",
    "Enables/disables compile-time queries across multiple files. SqlQuery/SqlExpression/SqlAction/SqlBatchAction instances used across files must be inline. They take advantage of a compile-time store called StoredXRs.db.",
    false
  ),
}
