package io.exoquery.util

import io.exoquery.config.ExoCompileOptions
import io.exoquery.util.Tracer
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

// NOTE if it wasn't for the buffer compilation would be blocked
// on every print-line of the tracer. Want to explore even better solutions
// that use suspended functions.
class FilePrintOutputSink private constructor(val writer: BufferedWriter) : Tracer.OutputSink {
  override fun output(str: String) {
    writer.write(str)
  }

  override fun close() {
    writer.flush()
    writer.close()
  }

  override fun flush() {
    writer.flush()
  }

  companion object {
    fun open(compileOptions: ExoCompileOptions) =
      open(compileOptions.projectDir)

    fun open(path: String): FilePrintOutputSink {
      val filePath = Path.of(path, "ExoQuery.log")
      val writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
      return FilePrintOutputSink(writer)
    }
  }
}
