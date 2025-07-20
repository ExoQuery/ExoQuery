package io.exoquery.codegen.gen

import io.exoquery.codegen.model.NameParser
import io.exoquery.codegen.model.AssemblingStrategy
import io.exoquery.codegen.model.NamingAnnotationType
import io.exoquery.codegen.model.NumericPreference
import io.exoquery.codegen.model.TableMeta
import io.exoquery.codegen.model.UnrecognizedTypeStrategy

typealias Namespacer = (TableMeta) -> String

data class PackagePath(val prefix: BasicPath, val innermost: String) {
  fun fullPath(): BasicPath = prefix + innermost
}

data class BasicPath(val path: List<String>) {
  constructor(vararg path: String) : this(path.toList())

  fun toPackageString(): String = path.joinToString(".")
  fun toPackageStringOrNull(): String? = if (path.isEmpty()) null else toPackageString()

  fun toDirPath(): String =
    path.joinToString(separator = "/", prefix = "", postfix = "")

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
  val rootPath: BasicPath,
  val packagePrefix: BasicPath = BasicPath(listOf()),
  val nameParser: NameParser = NameParser.LiteralNames,
  val defaultNamespace: String = "schema",
  val tableNamespacer: Namespacer = { it.tableSchema ?: defaultNamespace },
  val unrecognizedTypeStrategy: UnrecognizedTypeStrategy = UnrecognizedTypeStrategy.SkipColumn,
  val namingAnnotation: NamingAnnotationType = NamingAnnotationType.SerialName,
  val assemblingStrategy: AssemblingStrategy = AssemblingStrategy.SchemaPerPackage,
  val numericPreference: NumericPreference = NumericPreference.UseDefaults,
  // The default-name of the package or schema if one is not available from the database
  val defaultExcludedSchemas: Set<String> = setOf("information_schema", "performance_schema", "sys", "mysql"),
  val dryRun: Boolean = false
)
