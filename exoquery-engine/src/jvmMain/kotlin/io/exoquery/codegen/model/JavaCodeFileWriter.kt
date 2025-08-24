package io.exoquery.codegen.model

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

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

  override fun resetCodeRoot(path: String) {
    val rootPath = java.io.File(path)
    if (rootPath.exists() && rootPath.isFile)
      throw CodeGenerationError("Failed to clean code-generation root. The root path for code-generation is a file, but it must be a directory (or not exist at all): ${rootPath.absolutePath}")
    if (rootPath.exists())
      rootPath.deleteRecursively()
  }

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
