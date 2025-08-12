package io.exoquery.codegen.model

import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.CodeEmitterDeliverable
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.util.JdbcSchemaReader
import io.exoquery.codegen.util.SchemaReader
import io.exoquery.generation.CodeVersion
import io.ktor.http.content.VersionListProperty
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.code
import kotlin.reflect.KClass



data class JdbcWriteableFile(
  override val deliverable: CodeEmitterDeliverable,
  override val code: String,
  override val basePath: BasicPath
) : WriteableFile {
  fun fullPath(): Path = run {
    val fs = FileSystems.getDefault()
    val writePath = deliverable.makeWritePath(basePath)
    when (writePath.path.size) {
      0 -> throw IllegalArgumentException("Cannot write to a path with no segments: ${writePath.toDirPath()} for table ${deliverable.tables.map { it.name }} code file:\n${code.take(1000) + "..."}")
      1 -> throw IllegalArgumentException("Cannot write to a path with only one segment (file must at least have a directory): ${writePath.toDirPath()} for table ${deliverable.tables.map { it.name }} code file:\n${code.take(1000) + "..."}")
      else -> fs.getPath(fs.separator + writePath.path.joinToString(fs.separator))
    }
  }

  override fun print(): String =
    fullPath().toFile().toString()

  override fun write() {
    val fs = FileSystems.getDefault()
    val writePath = fullPath()
    val dirOfFile = writePath.parent.toFile()
    val exists = dirOfFile.exists()
    if (exists && !dirOfFile.isDirectory) {
      errorForDeliverable("The path for the code file is not a directory: ${dirOfFile.absolutePath}", this)
    }
    if (!exists && !dirOfFile.mkdirs()) {
      errorForDeliverable("Failed to create the directory for the code file: ${dirOfFile.absolutePath}", this)
    }
    writeToFile(this)
  }
}

private fun writeToFile(writable: JdbcWriteableFile) {
  try {
    val srcFile = writable.fullPath()
    Files.write(srcFile, writable.code.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
  } catch (e: Exception) {

  }
}

private fun errorForDeliverable(msg: String, writableFile: JdbcWriteableFile, parent: Throwable? = null): Nothing = run {
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

private fun writeToFileIfExists(writable: JdbcWriteableFile) {
  val srcFile = writable.fullPath()
  fun write() = writeToFile(writable)

  val fileExists = Files.exists(srcFile)
  when {
    !fileExists -> write()
    fileExists -> {
      val currentContents = Files.readString(srcFile)
      if (currentContents != writable.code) {
        writeToFile(writable)
      }
    }
    else -> Unit
  }
}

fun BasicPath.Companion.WorkingDir(): BasicPath {
  val workingDir = System.getProperty("user.dir")
  return BasicPath.SlashPath(workingDir)
}

abstract class JdbcGenerator(override val config: LowLevelCodeGeneratorConfig, open val allowUnknownDatabase: Boolean): GeneratorBase<Connection, ResultSet, JdbcWriteableFile>() {
  override fun kotlinTypeOf(cm: ColumnMeta): KClass<*>? =
    DefaultJdbcTyper(config.numericPreference).invoke(JdbcTypeInfo.fromColumnMeta(cm))

  override fun isNotNullable(cm: ColumnMeta): Boolean = cm.nullable == DatabaseMetaData.columnNullable

  abstract fun withConfig(config: LowLevelCodeGeneratorConfig): JdbcGenerator

  override fun buildFile(
    deliverable: CodeEmitterDeliverable,
    code: String,
    basePath: BasicPath
  ): JdbcWriteableFile =
    JdbcWriteableFile(deliverable, code, basePath)

  private val versionFile get() =
    File((config.rootPath + config.packagePrefix).toDirPath(), "CurrentVersion.kt")

  override fun readVersionFileIfPossible(): VersionFile? = run {
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

  override fun writeVersionFileIfNeeded() {
    // If we are actually configured to write a version file, write it
    (config.codeVersion as? CodeVersion.Fixed)?.let { codeVersion ->
      val versionFileConf = VersionFile(codeVersion.version)
      println("[ExoQuery] Codegen: Writing version-file `${codeVersion.version}` to ${versionFile.absolutePath}")
      versionFile.parentFile.mkdirs()
      Files.writeString(versionFile.toPath(), versionFileConf.serialize(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }
  }


  data class Live(override val config: LowLevelCodeGeneratorConfig, val connectionMaker: () -> Connection, override val allowUnknownDatabase: Boolean = false): JdbcGenerator(config, allowUnknownDatabase) {
    override val schemaReader = JdbcSchemaReader({ JdbcSchemaReader.Conn(connectionMaker()) }, allowUnknownDatabase)
    override fun withConfig(config: LowLevelCodeGeneratorConfig): JdbcGenerator =
      Live(config, connectionMaker, allowUnknownDatabase)
  }
}
