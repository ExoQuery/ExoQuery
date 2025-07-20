package io.exoquery.codegen.model

import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.CodeEmitter
import io.exoquery.codegen.gen.CodeEmitterDeliverable
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.gen.PackagePath
import io.exoquery.codegen.util.SchemaReader
import kotlin.reflect.KClass

interface WriteableFile {
  // Passed in by the Generator
  val deliverable: CodeEmitterDeliverable
  val code: String
  val basePath: BasicPath

  fun print(): String
  fun write(): Unit
}

data class GeneratorDeliverable<F: WriteableFile>(
  val databaseType: DatabaseTypes.DatabaseType,
  val files: List<F>
)

abstract class GeneratorBase<Conn: AutoCloseable, Results, F: WriteableFile> {
  abstract val config: LowLevelCodeGeneratorConfig
  protected abstract fun kotlinTypeOf(cm: ColumnMeta): KClass<*>?
  protected abstract fun makeConnection(): Conn
  protected abstract val schemaReader: SchemaReader<Conn, Results>
  protected abstract fun isNotNullable(cm: ColumnMeta): Boolean

  protected fun readMetas(): Pair<List<RawMeta>, DatabaseTypes.DatabaseType> =
    schemaReader.invoke { makeConnection() }

  protected fun filter(tc: RawMeta): Boolean = true

  data class TableGroup(val namespace: String?, val tables: List<TablePrepared>)

  private val packagePrefix get() = config.packagePrefix
  private val codeWrapperType get() = config.assemblingStrategy.codeWrapperType
  private val nameParser get() = config.nameParser

  protected abstract fun buildFile(
    deliverable: CodeEmitterDeliverable,
    code: String,
    basePath: BasicPath,
  ): F

  fun compute(): GeneratorDeliverable<F> {
    val (metasAll, databaseType) = readMetas()
    val metas = metasAll.filter { filter(it) }
    val processedTables =
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
                    throw IllegalArgumentException(
                      "Unrecognized type for column '${cm.columnName}' in table '${meta.table.tableName}': ${cm.dataType}"
                    )
                }
              }
            }
            .map { (cm, columnType) ->
              ColumnPrepared(
                nameParser.parseColumn(cm),
                columnType,
                isNotNullable(cm),
                cm
              )
            }

        TablePrepared(
          namespace,
          nameParser.parseTable(meta.table),
          columns,
          meta.table
        )
      }

    val grouped = groupByNamespace(processedTables)
    val deliverables = packageIntoDeliverables(grouped)
    val writableFiles = deliverables.map { buildFile(it, CodeEmitter(it).code, config.rootPath) }
    return GeneratorDeliverable<F>(databaseType, writableFiles)
  }

  fun run(): Unit {
    val deliverable = compute()
    deliverable.files.forEach { file ->
      println("Writing file: ${file.print()}")
      if (!config.dryRun)
        file.write()
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
}
