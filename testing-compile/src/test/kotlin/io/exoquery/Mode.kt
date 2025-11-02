package io.exoquery

import io.exoquery.annotation.ExoExtras

sealed interface Mode {
  val fileName: String

  data class ExoGoldenTest(override val fileName: String): Mode
  data class ExoGoldenOverride(override val fileName: String): Mode

  companion object {
    @ExoExtras
    fun ExoGoldenTest(): Mode =
      ExoGoldenTest(errorCap("No file name provided. This should be overridden by the compiler-plugin to ExoGoldenTestExpr"))

    fun ExoGoldenTestExpr(fileName: String): Mode = ExoGoldenTest(fileName)

    @ExoExtras
    fun ExoGoldenOverride(): Mode =
      ExoGoldenOverride(errorCap("No file name provided. This should be overridden by the compiler-plugin to ExoGoldenOverrideExpr"))

    fun ExoGoldenOverrideExpr(fileName: String): Mode = ExoGoldenOverride(fileName)
  }
}
