package io.exoquery.codegen.model

import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.CodeEmitterDeliverable
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.gen.RootedPath
import io.exoquery.codegen.util.JdbcSchemaReader
import io.exoquery.codegen.util.SchemaReader
import io.exoquery.codegen.util.SchemaReaderTest
import io.exoquery.generation.CodeVersion
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import kotlin.reflect.KClass

class JavaCodeFileWriter : CodeFileWriter {
  private fun CodeFile.fullPath(): Path = run {
    val fs = FileSystems.getDefault()
    val writePath = makeWritePath()
    when (writePath.parts.size) {
      0 -> throw IllegalArgumentException("Cannot write to a path with no segments: ${writePath.toDirPath()} for table ${deliverable.tables.map { it.name }} code file:\n${code.take(1000) + "..."}")
      1 -> throw IllegalArgumentException("Cannot write to a path with only one segment (file must at least have a directory): ${writePath.toDirPath()} for table ${deliverable.tables.map { it.name }} code file:\n${code.take(1000) + "..."}")
      // assuming it's a absolute path at this point (since the root-path has been tacked on to the code-file path)
      // or perhaps should BasicPath have a concept of root-path segments?
      else -> fs.getPath(writePath.parts.joinToString(fs.separator))
    }
  }

  override fun printPath(writableFile: CodeFile): String =
    writableFile.fullPath().toFile().toString()

  override fun write(writableFile: CodeFile) {
    val fs = FileSystems.getDefault()
    val writePath = writableFile.fullPath()
    val dirOfFile = writePath.parent.toFile()
    val exists = dirOfFile.exists()
    if (exists && !dirOfFile.isDirectory) {
      errorForDeliverable("The path for the code file is not a directory: ${dirOfFile.absolutePath}", writableFile)
    }
    if (!exists && !dirOfFile.mkdirs()) {
      errorForDeliverable("Failed to create the directory for the code file: ${dirOfFile.absolutePath}", writableFile)
    }
    Files.write(writePath, writableFile.code.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  }
}

private fun errorForDeliverable(msg: String, writableFile: CodeFile, parent: Throwable? = null): Nothing = run {
  val deliverable = writableFile.deliverable
  val code = writableFile.code
  throw IllegalStateException(
    """|${msg}
       |================ Tables ================ 
       |${deliverable.tables.map { it.namespace + "->" + it.name }.joinToString("\n")}
       |================= Code =================
       |${code.take(1000) + if (code.length > 1000) "..." else ""}
       """.trimMargin(),
    parent
  )
}

fun BasicPath.Companion.WorkingDir(): BasicPath {
  val workingDir = System.getProperty("user.dir")
  return BasicPath.SlashPath(workingDir)
}

object JavaVersionFileWriter: VersionFileWriter {

  private fun makeVersionFile(config: LowLevelCodeGeneratorConfig) = run {
    val rootedPath = RootedPath(config.rootPath, config.packagePrefix)
    File(rootedPath.toDirPath(), "CurrentVersion.kt")
  }

  override fun readVersionFileIfPossible(config: LowLevelCodeGeneratorConfig): VersionFile? = run {
    val versionFile = makeVersionFile(config)
    if (versionFile.exists()) {
      try {
        val body = Files.readString(versionFile.toPath())
        VersionFile.parse(body)
      } catch (e: Exception) {
        throw IllegalStateException("Failed to read version file at ${versionFile.absolutePath}", e)
      }
    } else {
      null
    }
  }

  override fun writeVersionFileIfNeeded(config: LowLevelCodeGeneratorConfig) {
    val versionFile = makeVersionFile(config)
    (config.codeVersion as? CodeVersion.Fixed)?.let { codeVersion ->
      val versionFileConf = VersionFile(codeVersion.version)
      println("[ExoQuery] Codegen: Writing version-file `${codeVersion.version}` to ${versionFile.absolutePath}")
      versionFile.parentFile.mkdirs()
      Files.writeString(versionFile.toPath(), versionFileConf.serialize(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }
  }
}

abstract class JdbcGenerator(
  override val config: LowLevelCodeGeneratorConfig,
  open val allowUnknownDatabase: Boolean
): GeneratorBase<Connection, ResultSet>() {
  override fun defaultKotlinTypeOf(cm: ColumnMeta): ClassName? =
    DefaultJdbcTyper(config.numericPreference).invoke(JdbcTypeInfo.fromColumnMeta(cm))

  override fun isNullable(cm: ColumnMeta): Boolean = cm.nullable == DatabaseMetaData.columnNullable

  abstract fun withConfig(config: LowLevelCodeGeneratorConfig): JdbcGenerator

  override fun buildFile(
    deliverable: CodeEmitterDeliverable,
    code: String,
    basePath: String
  ): CodeFile =
    CodeFile(deliverable, code, basePath)

  data class Live(
    override val config: LowLevelCodeGeneratorConfig,
    val connectionMaker: () -> Connection,
    override val allowUnknownDatabase: Boolean = false
  ): JdbcGenerator(config, allowUnknownDatabase) {
    override val schemaReader = JdbcSchemaReader({ JdbcSchemaReader.Conn(connectionMaker()) }, allowUnknownDatabase)
    override val fileWriter: CodeFileWriter = JavaCodeFileWriter()
    override fun withConfig(config: LowLevelCodeGeneratorConfig): JdbcGenerator = Live(config, connectionMaker, allowUnknownDatabase)
    override val versionFileWriter: VersionFileWriter = JavaVersionFileWriter
  }

  data class Test(
    override val config: LowLevelCodeGeneratorConfig,
    override val schemaReader: SchemaReader,
    override val allowUnknownDatabase: Boolean = false,
    override val fileWriter: CodeFileWriter.Test = CodeFileWriter.Test(),
    override val versionFileWriter: VersionFileWriter.Test = VersionFileWriter.Test()
  ): JdbcGenerator(config, allowUnknownDatabase) {
    constructor(
      config: LowLevelCodeGeneratorConfig,
      testSchemaReaderTest: SchemaReaderTest.TestSchema,
      allowUnknownDatabase: Boolean = false,
      fileWriter: CodeFileWriter.Test = CodeFileWriter.Test(),
      versionFileWriter: VersionFileWriter.Test = VersionFileWriter.Test()
    ): this(
        config,
        SchemaReaderTest(testSchemaReaderTest, allowUnknownDatabase),
        allowUnknownDatabase,
        fileWriter,
        versionFileWriter
      )

    override fun withConfig(config: LowLevelCodeGeneratorConfig): JdbcGenerator = Test(config, schemaReader, allowUnknownDatabase, fileWriter)
  }
}
