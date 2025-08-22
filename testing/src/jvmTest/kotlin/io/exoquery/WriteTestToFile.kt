package io.exoquery

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

actual fun writeToFile(originalFilePath: String, newFileName: String, contents: String, override: Boolean) {
  val parentDir = Path.of(originalFilePath).parent
  val fullPath = parentDir.resolve(newFileName + ".kt")
  // If the file does not exist or if we are supposed to override it
  if (!Files.exists(fullPath) || override)
    Files.write(fullPath, contents.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
}
