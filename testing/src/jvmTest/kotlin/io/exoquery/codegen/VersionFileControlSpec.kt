package io.exoquery.codegen

import io.exoquery.codegen.model.CodeFileWriter
import io.exoquery.codegen.model.JdbcGenerator
import io.exoquery.codegen.model.VersionFile
import io.exoquery.codegen.model.VersionFileWriter
import io.exoquery.generation.Code
import io.exoquery.generation.CodeVersion
import io.exoquery.generation.DatabaseDriver
import io.exoquery.generation.toLowLevelConfig
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.sql.JDBCType

class VersionFileControlSpec: FreeSpec({
  val schema =
    listOf(
      TableMock(
        name = "test_table",
        schema = "myschema",
        columns = listOf(
          ColumnMock("id", JDBCType.BIGINT, false),
          ColumnMock("first_name", JDBCType.VARCHAR, true)
        )
      )
    ).toSchema()

  fun schemaWithVersion(codeVersion: CodeVersion) =
    Code.DataClasses(
      codeVersion,
      DatabaseDriver.Postgres(),
      packagePrefix = "foo.bar",
      tableFilter = "test_table"
    ).toLowLevelConfig("/my/drive", null).first

  fun schemaWithFixedVersion(version: String) =
    schemaWithVersion(CodeVersion.Fixed(version))

  fun schemaWithFloatingVersion() =
    schemaWithVersion(CodeVersion.Floating)

  val expectedContent =
    CodeFileContent(
      "/my/drive/foo/bar/myschema/test_table.kt",
      "foo.bar.myschema",
      """
      @Serializable
      data class test_table(val id: Long, val first_name: String?)
      """.trimIndent()
    )

  "Fixed Version + file-not-exists -> file-exists (and regen) -> no-change, file-updated -> regen" {
    val fileWriter = CodeFileWriter.Test()
    val versionFileWriter = VersionFileWriter.Test()

    JdbcGenerator.Test(schemaWithFixedVersion("1.0.0"), schema, fileWriter = fileWriter, versionFileWriter = versionFileWriter).run()
    fileWriter.getWrittenFiles().single().toContent() shouldBe expectedContent
    versionFileWriter.getWrittenVersionFiles().single().second shouldBe VersionFile("1.0.0").serialize()

    fileWriter.clear()
    JdbcGenerator.Test(schemaWithFixedVersion("1.0.0"), schema, fileWriter = fileWriter, versionFileWriter = versionFileWriter).run()
    fileWriter.getWrittenFiles().shouldBeEmpty()
    versionFileWriter.getWrittenVersionFiles().single().second shouldBe VersionFile("1.0.0").serialize()

    JdbcGenerator.Test(schemaWithFixedVersion("1.0.1"), schema, fileWriter = fileWriter, versionFileWriter = versionFileWriter).run()
    fileWriter.getWrittenFiles().single().toContent() shouldBe expectedContent
    versionFileWriter.getWrittenVersionFiles().map { it.second } shouldBe listOf(VersionFile("1.0.0").serialize(), VersionFile("1.0.1").serialize())
  }
  "Fixed Version + file-exists -> no-change" {
    val fileWriter = CodeFileWriter.Test()
    val versionFileWriter = VersionFileWriter.Test()
    versionFileWriter.writeVersionFileIfNeeded(schemaWithFixedVersion("1.0.0"))

    JdbcGenerator.Test(schemaWithFixedVersion("1.0.0"), schema, fileWriter = fileWriter, versionFileWriter = versionFileWriter).run()
    fileWriter.getWrittenFiles().shouldBeEmpty()
    versionFileWriter.getWrittenVersionFiles().single().second shouldBe VersionFile("1.0.0").serialize()
  }
  "Fixed Version + file-exists + forcedRegen -> regen" {
    val fileWriter = CodeFileWriter.Test()
    val versionFileWriter = VersionFileWriter.Test()
    versionFileWriter.writeVersionFileIfNeeded(schemaWithFixedVersion("1.0.0"))

    JdbcGenerator.Test(schemaWithFixedVersion("1.0.0"), schema, fileWriter = fileWriter, versionFileWriter = versionFileWriter).run(forceRegen = true)
    fileWriter.getWrittenFiles().single().toContent() shouldBe expectedContent
    versionFileWriter.getWrittenVersionFiles().map { it.second } shouldBe listOf(VersionFile("1.0.0").serialize(), VersionFile("1.0.0").serialize())
  }
  "Floating Version: file-not-exists, file-not-exists (and regen)" {
    val fileWriter = CodeFileWriter.Test()
    val versionFileWriter = VersionFileWriter.Test()

    JdbcGenerator.Test(schemaWithFloatingVersion(), schema, fileWriter = fileWriter, versionFileWriter = versionFileWriter).run()
    fileWriter.getWrittenFiles().single().toContent() shouldBe expectedContent
    versionFileWriter.getWrittenVersionFiles().shouldBeEmpty()

    fileWriter.clear()
    JdbcGenerator.Test(schemaWithFloatingVersion(), schema, fileWriter = fileWriter, versionFileWriter = versionFileWriter).run()
    fileWriter.getWrittenFiles().single().toContent() shouldBe expectedContent
    versionFileWriter.getWrittenVersionFiles().shouldBeEmpty()
  }
  "Floating Version + File Exists -> regen" {
    val fileWriter = CodeFileWriter.Test()
    val versionFileWriter = VersionFileWriter.Test()
    versionFileWriter.writeVersionFileIfNeeded(schemaWithFixedVersion("1.0.0"))

    JdbcGenerator.Test(schemaWithFloatingVersion(), schema, fileWriter = fileWriter, versionFileWriter = versionFileWriter).run()
    fileWriter.getWrittenFiles().single().toContent() shouldBe expectedContent
  }
})
