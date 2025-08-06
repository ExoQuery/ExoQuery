package io.exoquery.plugin.transform

import io.exoquery.config.ExoCompileOptions
import io.exoquery.plugin.logging.CompileLogger
import io.exoquery.printing.PrintableValue
import io.exoquery.printing.QueryFileKotlinMaker
import io.exoquery.printing.QueryFileKotlinMakerRoom
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrFile
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.nameWithoutExtension


class QueryFile(
  // I.e. the actual file encountered by the Ir transformer
  val codeFile: IrFile,
  val codeFileAccum: FileQueryAccum,
  val compilerConfig: CompilerConfiguration,
  val exoOptions: ExoCompileOptions
) {
  private val fs by lazy { FileSystems.getDefault() }
  private val logger by lazy { CompileLogger(compilerConfig, codeFile, codeFile) }

  fun buildRoomFile() {

    // e.g: /home/me/project/src/commonMain/com/someplace/Code.kt -> /home/me/project/src/commonMain/kotlin/com/someplace/
    val codeFileParent = Path.of(codeFile.fileEntry.name).parent
    // projectSrcPath: /home/me/project/src/
    // exoRoomPath: /home/me/project/src/exoroom/kotlin (TODO this should be configureable in the gradle plugin and should automatically be added as a source directory if possible)
    val exoRoomPath = Path.of(exoOptions.projectSrcDir, "exoroom", "kotlin")
    val packageSubpath = codeFile.packageFqName.pathSegments().joinToString(fs.separator)
    val fileNameWithoutExtension = Path.of(codeFile.fileEntry.name).nameWithoutExtension
    val srcFileName = fileNameWithoutExtension + "RoomQueries"
    val pathOfFile = exoRoomPath.resolve(packageSubpath)
    val codeFilePath = pathOfFile.resolve(srcFileName + ".kt")
    val dirOfFile = pathOfFile.toFile()

    if (!dirOfFile.dirExistsOrCouldMake()) {
      logger.error("Failed to create the directory for the Room queries: ${dirOfFile.absolutePath}")
      return // failing the build, return without creating the file
    }

    val collectedQueries = codeFileAccum.currentQueries()

    val dumpedQueryText = QueryFileKotlinMakerRoom.invoke(collectedQueries.map { it.toPrintableValue() }, srcFileName, codeFile.packageFqName.asString())
    writeToFileIfExists(dumpedQueryText, codeFilePath, "(to override set the annotation to ExoGoldenOverride)")
  }

  fun buildRegular() {
    // e.g: /home/me/project/src/commonMain/com/someplace/Code.kt -> /home/me/project/src/commonMain/kotlin/com/someplace/
    val codeFileParent = Path.of(codeFile.fileEntry.name).parent
    // projectDir: /home/me/project/
    val projectDirPath = Path.of(exoOptions.projectDir)
    val (pathOfFile, regularProcess) =
      if (codeFileParent.startsWith(projectDirPath)) {
        val trimmedPath =
          projectDirPath.relativize(codeFileParent) // this should be: src/commonMain/kotlin/com/someplace/
            // we expect it to start with src so remove that
            .let { if (it.startsWith("src")) Path.of("src").relativize(it) else it } // now should be: commonMain/kotlin/com/someplace/
        // fileGenerationPath: /home/me/project/build/generated/exo
        val fileGenerationPath = Path.of(exoOptions.queriesBaseDir)
        // outputPath: /home/me/project/build/generated/exo/ + src/commonMain/kotlin/com/someplace/
        val outputPath = fileGenerationPath.resolve(trimmedPath)
        outputPath to true
      } else {
        val packageName = codeFile.packageFqName.asString().replace(".", fs.separator)
        // fileGenerationPath: /home/me/project/build/generated/exo
        val fileGenerationPath = Path.of(exoOptions.queriesBaseDir)
        // fileGenerationPath: /home/me/project/build/generated/exo/linuxX64/linuxX64Main/com/someplace/
        val outputPath = fileGenerationPath.resolve(exoOptions.targetName).resolve(exoOptions.sourceSetName).resolve(Path.of(packageName))
        outputPath to false
      }
    val dirOfFile = pathOfFile.toFile()

    val srcFileName = Path.of(codeFile.fileEntry.name).nameWithoutExtension + ".queries.sql"

    if (!dirOfFile.dirExistsOrCouldMake()) return // failing the build, return without creating the file

    val srcFile = Path.of(dirOfFile.toString(), srcFileName)

    val collectedQueries = codeFileAccum.currentQueries()
    val dumpedQueryText = QueryFileTextMaker(collectedQueries, AccumState.PathBehavior.IncludePaths, AccumState.LabelBehavior.IncludeAll)
    writeToFileIfExists(dumpedQueryText, srcFile, "(to override set the annotation to ExoGoldenOverride)")
  }

  private fun writeToFileIfExists(dumpedQueryText: String, srcFile: Path, addendum: String) {
    fun write() = writeToFile(dumpedQueryText, srcFile)

    val fileExists = Files.exists(srcFile)
    when {
      !fileExists -> write()
      fileExists -> {
        val currentContents = Files.readString(srcFile)
        if (currentContents != dumpedQueryText) {
          writeToFile(dumpedQueryText, srcFile)
        }
      }
      else ->
        logger.warn("File already exists, not overriding it: ${srcFile}.${if (addendum.isNotEmpty()) " $addendum" else ""}")
    }
  }

  fun buildForGoldenFile(overrwriteExisting: Boolean) {
    // e.g: /home/me/project/src/commonMain/com/someplace/Code.kt -> /home/me/project/src/commonMain/com/someplace/
    val codeFilePath = Path.of(codeFile.fileEntry.name)
    val codeFileParent = codeFilePath.parent

    // dirOfFile: /home/me/project/src/commonMain/com/someplace/
    val dirOfFile = codeFileParent.toFile()

    val srcFileName = Path.of(codeFile.fileEntry.name).nameWithoutExtension + "Golden.kt"

    if (!dirOfFile.dirExistsOrCouldMake()) return // failing the build, return without creating the file

    val srcFile = Path.of(dirOfFile.toString(), srcFileName)

    val currentQueries = codeFileAccum.currentQueries().filter { it.label != null }

    // Since we can only golden tests that are actually labelled, need to make sure the queries have labels
    if (currentQueries.isEmpty()) {
      logger.warn("No queries found in file: ${codeFile.fileEntry.name}. Not writing queries to it.")
      return
    }

    val dumpedQueries = QueryFileKotlinMaker.invoke(
      currentQueries.map { it.toPrintableValue() }, codeFilePath.nameWithoutExtension + "Golden", codeFile.packageFqName.asString()
    )
    val fileExists = Files.exists(srcFile)
    fun write() = writeToFile(dumpedQueries, srcFile)

    when {
      !fileExists -> write()
      fileExists && overrwriteExisting -> {
        val currentContents = Files.readString(srcFile)
        if (currentContents != dumpedQueries) {
          logger.warn("Overriding the golden file: ${srcFile} (to disable override, set to ExoGoldenTest)")
          write()
        }
      }
      else ->
        logger.warn("File already exists, not overriding it: ${srcFile} (to override set the annotation to ExoGoldenOverride).")
    }
  }

  private fun File.dirExistsOrCouldMake() =
    if (!this.exists() && !this.mkdirs()) {
      logger.warn("Failed to create the parent directory: ${this.absolutePath}")
      false
    } else {
      true
    }

  private fun writeToFile(queries: String, srcFile: Path) {
    try {
      Files.write(srcFile, queries.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    } catch (e: Exception) {

      logger.error(
        """|Could not write Queries to file: ${srcFile.toAbsolutePath()}
           |================= Error: =================
           |${e.stackTraceToString()}
           |================= Queries
           |${queries}
           """.trimMargin()
      )
    }
  }

  private fun PrintableQuery.toPrintableValue() = PrintableValue(query, PrintableValue.Type.SqlQuery(xr), queryOutput, label)
}
