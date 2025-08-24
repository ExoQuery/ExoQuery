package io.exoquery.codegen.model

import io.exoquery.codegen.gen.BasicPath
import io.exoquery.codegen.gen.CodeEmitterDeliverable
import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.util.JdbcSchemaReader
import io.exoquery.codegen.util.SchemaReader
import io.exoquery.codegen.util.SchemaReaderTest
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet

internal fun errorForDeliverable(msg: String, writableFile: CodeFile, parent: Throwable? = null): Nothing = run {
  val deliverable = writableFile.deliverable
  val code = writableFile.code
  throw CodeGenerationError(
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
