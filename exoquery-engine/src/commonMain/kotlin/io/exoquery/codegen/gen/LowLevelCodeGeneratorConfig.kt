package io.exoquery.codegen.gen

import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.AssemblingStrategy
import io.exoquery.codegen.model.NamingAnnotationType
import io.exoquery.codegen.model.NumericPreference
import io.exoquery.codegen.model.TableMeta
import io.exoquery.codegen.model.UnrecognizedTypeStrategy
import io.exoquery.generation.CodeVersion
import kotlin.collections.plus

typealias Namespacer = (TableMeta) -> String

data class PackagePath(val prefix: BasicPath, val innermost: String) {
  fun fullPath(): BasicPath = prefix + innermost
}

data class RootedPath(val root: String, val rel: BasicPath) {
  constructor(root: String, vararg rel: String) : this(root, BasicPath(rel.toList()))

  fun addFileExtension(extension: String): RootedPath =
    if (rel.path.isEmpty()) this
    else RootedPath(root, rel.addFileExtension(extension))

  /** Combine the absolute root path with the relative path. This is not really a 'path' the way would think of it
   * but it is needed for actually doing useful things with the rooted path.
   */
  val parts = listOf(root) + rel.path

  fun toDirPath(): String =
    parts.joinToString(separator = "/", prefix = "", postfix = "")

  operator fun plus(other: BasicPath): RootedPath = RootedPath(root, rel + other)
  operator fun plus(other: String): RootedPath = RootedPath(root, rel + other)
}

data class BasicPath(val path: List<String>) {
  constructor(vararg path: String) : this(path.toList())

  fun toPackageString(): String = path.joinToString(".")
  fun toPackageStringOrNull(): String? = if (path.isEmpty()) null else toPackageString()

  fun toDirPath(): String =
    path.joinToString(separator = "/")

  fun addFileExtension(extension: String): BasicPath =
    if (path.isEmpty()) this
    else BasicPath(path.dropLast(1) + (path.last() + "." + extension))

  operator fun plus(other: BasicPath): BasicPath = BasicPath(path + other.path)
  operator fun plus(other: String): BasicPath = BasicPath(path + other)

  companion object {
    fun DotPath(path: String): BasicPath =
      BasicPath(path.split('.').map { it.trim() }.filter { it.isNotEmpty() })
    fun SlashPath(path: String): BasicPath =
      BasicPath(path.split(Regex("[\\,/]+")).map { it.trim() }.filter { it.isNotEmpty() })

    val Empty = BasicPath(listOf())

    // Need this to be able to use BasicPath.WorkingDir()
  }
}

data class LowLevelCodeGeneratorConfig(
  val codeVersion: CodeVersion,
  val rootPath: String,
  val packagePrefix: BasicPath = BasicPath(listOf()),
  val nameParser: NameParser = NameParser.Literal,
  val defaultNamespace: String = "schema",
  val tableNamespacer: Namespacer = { it.tableSchema ?: defaultNamespace },
  val unrecognizedTypeStrategy: UnrecognizedTypeStrategy = UnrecognizedTypeStrategy.ThrowTypingError,
  val namingAnnotation: NamingAnnotationType = NamingAnnotationType.SerialName,
  val assemblingStrategy: AssemblingStrategy = AssemblingStrategy.SchemaPerPackage,
  val numericPreference: NumericPreference = NumericPreference.PreferPrimitivesWhenPossible,
  // The default-name of the package or schema if one is not available from the database
  val schemaFilter: String? = null,
  val tableFilter: String? = null,
  val defaultExcludedSchemas: Set<String> = setOf("information_schema", "performance_schema", "sys", "mysql"),
  val rootLevelOpenApiKey: String? = null,
  val dryRun: Boolean = false,
  val detailedLogs: Boolean = false
)
