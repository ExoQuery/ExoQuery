package io.exoquery.plugin.transform

import io.exoquery.ExoRoomInterface
import io.exoquery.annotation.ExoGoldenOverride
import io.exoquery.annotation.ExoGoldenTest
import io.exoquery.plugin.hasAnnotation

object QueryFileBuilder {
  sealed interface OutputMode {
    object Regular : OutputMode
    object RoomQueryFile : OutputMode // TODO do we want a selearte mode for Room no-overwrite?
    object GoldenOverwrite : OutputMode
    object GoldenNoOverwrite : OutputMode
  }

  context(scope: CX.Scope) operator fun invoke(queryFile: QueryFile) {
    if (!queryFile.codeFileAccum.hasQueries()) return

    // check queries for duplicate labels
    val labelDups = queryFile.codeFileAccum.currentQueries().filterNot { it.label == null }.groupBy { it.label }.filter { it.value.size > 1 }
    if (labelDups.isNotEmpty()) {
      scope.logger.error("Duplicate labels found in queries: ${labelDups.keys.joinToString(", ")}")
      return
    }

    val specialFileWriteType =
      if (queryFile.codeFile.hasAnnotation<ExoRoomInterface>())
        OutputMode.RoomQueryFile
      else if (queryFile.codeFile.hasAnnotation<ExoGoldenOverride>())
        OutputMode.GoldenOverwrite
      else if (queryFile.codeFile.hasAnnotation<ExoGoldenTest>())
        OutputMode.GoldenNoOverwrite
      else
        null

    // either way write the queries out to the build directory (if enabled)
    if (scope.options?.queryFilesEnabled ?: false) {
      writeFile(OutputMode.Regular, queryFile)
    }
    // if the resourcesWrite is defined, write the queries out to the resources directory
    if (specialFileWriteType != null) {
      writeFile(specialFileWriteType, queryFile)
    }
  }

  private fun writeFile(outputMode: OutputMode, queryFile: QueryFile) =
    when (outputMode) {
      OutputMode.Regular -> queryFile.buildRegular()
      OutputMode.GoldenOverwrite -> queryFile.buildForGoldenFile(overrwriteExisting = true)
      OutputMode.GoldenNoOverwrite -> queryFile.buildForGoldenFile(overrwriteExisting = false)
      OutputMode.RoomQueryFile -> queryFile.buildRoomFile()
    }
}
