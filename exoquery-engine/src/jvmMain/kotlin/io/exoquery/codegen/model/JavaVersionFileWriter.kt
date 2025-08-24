package io.exoquery.codegen.model

import io.exoquery.codegen.gen.LowLevelCodeGeneratorConfig
import io.exoquery.codegen.gen.RootedPath
import io.exoquery.generation.CodeVersion
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

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
