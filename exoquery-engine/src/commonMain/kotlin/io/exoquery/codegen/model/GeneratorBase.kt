package io.exoquery.codegen.model

import io.exoquery.codegen.gen.CodeEmitter
import io.exoquery.codegen.gen.CodeEmitterDeliverable
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.gen.PackagePath
import io.exoquery.codegen.util.SchemaReader
import io.exoquery.generation.CodeVersion
import kotlin.reflect.KClass

class CodeGenerationError(override val message: String, cause: Exception? = null) : RuntimeException(message, cause)

data class BasePath(val value: String)

data class CodeFile(
  // Passed in by the Generator
  val deliverable: CodeEmitterDeliverable,
  val code: String,
  val basePath: String
) {
  fun makeWritePath() = deliverable.makeWritePath(basePath)
}

interface CodeFileWriter {
  fun write(writableFile: CodeFile): Unit
  fun printPath(writableFile: CodeFile): String

  class Test : CodeFileWriter {
    private val writtenFiles = mutableListOf<CodeFile>()

    override fun write(writableFile: CodeFile) {
      writtenFiles.add(writableFile)
    }

    override fun printPath(writableFile: CodeFile): String =
      writableFile.makeWritePath().toDirPath()

    fun getWrittenFiles(): List<CodeFile> = writtenFiles

    fun clear() {
      writtenFiles.clear()
    }
  }
}

data class GeneratorDeliverable(
  val databaseType: DatabaseTypes.DatabaseType,
  val files: List<CodeFile>
)

interface VersionFileWriter {
  fun readVersionFileIfPossible(config: LowLevelCodeGeneratorConfig): VersionFile?
  fun writeVersionFileIfNeeded(config: LowLevelCodeGeneratorConfig): Unit

  class Test: VersionFileWriter {

    fun currentVersionFile(config: LowLevelCodeGeneratorConfig): VersionFile? =
      when (val codeVersion = config.codeVersion) {
        is CodeVersion.Fixed -> VersionFile(codeVersion.version)
        else -> null
      }

    private val writtenVersionFiles = mutableListOf<Pair<VersionFile, String>>()
    fun getWrittenVersionFiles(): List<Pair<VersionFile, String>> = writtenVersionFiles

    override fun readVersionFileIfPossible(config: LowLevelCodeGeneratorConfig): VersionFile? =
      writtenVersionFiles.lastOrNull()?.let { VersionFile.parse(it.second) }
    override fun writeVersionFileIfNeeded(config: LowLevelCodeGeneratorConfig): Unit {
      currentVersionFile(config)?.let { versionFile ->
        writtenVersionFiles.add(versionFile to versionFile.serialize())
      }
    }
  }
}

abstract class GeneratorBase<Conn: AutoCloseable, Results> {
  abstract val config: LowLevelCodeGeneratorConfig
  abstract val fileWriter: CodeFileWriter
  abstract val versionFileWriter: VersionFileWriter

  protected abstract fun kotlinTypeOf(cm: ColumnMeta): KClass<*>?
  protected abstract val schemaReader: SchemaReader
  protected abstract fun isNullable(cm: ColumnMeta): Boolean

  protected fun readMetas(): Pair<List<RawTableMeta>, DatabaseTypes.DatabaseType> = run {
    val (tableMetas, databaseType) = schemaReader.readSchemas()
    val schemaFilterRegex = config.schemaFilter?.toRegex()
    val tableFilterRegex = config.tableFilter?.toRegex()
    val schemaFilter = { str: String -> schemaFilterRegex?.matches(str) ?: true }
    val tableFilter = { str: String -> tableFilterRegex?.matches(str) ?: true }

    val filteredMetas =
      tableMetas.filter {
        it.table.tableCat?.let { schemaFilter(it) } ?: true && tableFilter(it.table.tableName)
      }

    filteredMetas to databaseType
  }

  // TODO unknown-column type behavior needs to be exposed in Code.DataClasses


  protected fun filter(tc: RawTableMeta): Boolean = true

  data class TableGroup(val namespace: String?, val tables: List<TablePrepared>)

  private val packagePrefix get() = config.packagePrefix
  private val codeWrapperType get() = config.assemblingStrategy.codeWrapperType
  private val nameParser get() = config.nameParser

  protected abstract fun buildFile(
    deliverable: CodeEmitterDeliverable,
    code: String,
    basePath: String,
  ): CodeFile

  fun compute(): GeneratorDeliverable {
    val (metasAll, databaseType) = readMetas()
    val metas = metasAll.filter { filter(it) }
    val rawTables =
      metas.map { meta ->
        val namespace = config.tableNamespacer(meta.table)
        val columns =
          meta.columns
            .mapNotNull { cm ->
              kotlinTypeOf(cm)?.let { cm to it } ?: run {
                when (config.unrecognizedTypeStrategy) {
                  UnrecognizedTypeStrategy.AssumeString -> cm to String::class
                  UnrecognizedTypeStrategy.SkipColumn -> null
                  UnrecognizedTypeStrategy.ThrowTypingError ->
                    throw CodeGenerationError(
                      "Unrecognized type for column '${cm.columnName}' in table '${meta.table.tableName}': ${cm.dataType}"
                    )
                }
              }
            }
            .map { (cm, columnType) ->
              ColumnPrepared(
                cm.columnName,
                columnType,
                isNullable(cm),
                cm
              )
            }

        TablePrepared(
          namespace,
          meta.table.tableName,
          columns,
          meta.table
        )
      }

    val processedTables = nameParser.parseTables(rawTables, ProcessingContext(config.detailedLogs, config.rootLevelOpenApiKey))
    val grouped = groupByNamespace(processedTables)
    val deliverables = packageIntoDeliverables(grouped)
    val writableFiles = deliverables.map { buildFile(it, CodeEmitter(it).code, config.rootPath) }
    return GeneratorDeliverable(databaseType, writableFiles)
  }

  fun isCurrentVersion(versionFile: VersionFile): Boolean =
    (config.codeVersion as? CodeVersion.Fixed)?.let { it.version == versionFile.currentVersion } ?: run {
      println("[ExoQuery] Codegen: ignoring existing version file, as code version is not fixed.")
      false
    }

  fun run(): Unit {
    val foundVersionFile: VersionFile? = versionFileWriter.readVersionFileIfPossible(config)
    val isCurrentVersion = foundVersionFile?.let { isCurrentVersion(it) } ?: false
    if (!isCurrentVersion) {
      val deliverable = compute()
      deliverable.files.forEach { file ->
        println("[ExoQuery] Codegen: Writing file: ${fileWriter.printPath(file)}")
        if (!config.dryRun)
          fileWriter.write(file)
      }
      versionFileWriter.writeVersionFileIfNeeded(config)
    } else {
      println("[ExoQuery] Codegen: skipping code generation, current version ${foundVersionFile.currentVersion} is up to date.")
    }
  }

  protected fun packageIntoDeliverables(tableGroups: List<TableGroup>) =
    tableGroups
      .map { grp ->
        val packagePath = PackagePath(packagePrefix, grp.namespace ?: config.defaultNamespace)
        val wrapper = codeWrapperType.makeWrapper(packagePath)
        CodeEmitterDeliverable(packagePath, grp.tables, wrapper, config.namingAnnotation)
      }

  protected fun groupByNamespace(tables: List<TablePrepared>): List<TableGroup> =
    when (config.assemblingStrategy.schemaGroupingStrategy) {
      SchemaGroupingStrategy.GroupBySchema -> {
        tables
          .groupBy { it.namespace }
          .map { (namespace, tables) ->
            TableGroup(namespace, tables)
          }
      }
      SchemaGroupingStrategy.DoNotGroup -> {
        tables
          .map { TableGroup(it.namespace, listOf(it)) }
      }
    }

  companion object {
  }
}

data class VersionFile(val currentVersion: String) {
  fun serialize() =
    """
    |// This file is generated by the code generator. Do NOT edit it manually.
    |// You can remove it if you want to force code-regeneration.
    |
    |val currentVersion = "$currentVersion"
    """.trimMargin()

  companion object {
    fun parse(versionFileBody: String) =
      versionFileBody
        .lines()
        .firstOrNull { it.startsWith("val currentVersion =") }
        ?.substringAfter("val currentVersion =")
        ?.trim()
        ?.removeSurrounding("\"")
        ?.let { VersionFile(it) }
        ?: throw IllegalArgumentException("Invalid version file format\n========== Version File: ===========\n$versionFileBody")
  }
}
