package io.exoquery

import org.gradle.api.Project
import org.gradle.api.file.Directory
import java.io.File

object ExoPlugin {
  val RelativeOutputPath = "generated/exoquery/"
}

val Project.generatedRootDir get() =
  this.layout.buildDirectory.dir(ExoPlugin.RelativeOutputPath)

val Project.generatedSqlDir get() =
  generatedRootDir.map { it.dir("sql") }

sealed interface EntitiesDirType {
  data object Regular : EntitiesDirType
  data object Permanent : EntitiesDirType
  data object Custom : EntitiesDirType
}

class GeneratedEntitiesDirConventions(val ext: ExoQueryGradlePluginExtension, val project: Project) {

  fun show(project: Project, sourceSetName: String, target: String) {
    println(
      """
      ====================== Conventions for [${project.name}] $target->$sourceSetName ======================
      generatedRootDir: ${project.generatedRootDir.orNull}
      generatedSqlDir: ${project.generatedSqlDir.orNull}
      generatedEntitiesDir: ${generatedEntitiesDir(project).first.orNull}
      generatedEntitiesSubdir: ${generatedEntitiesSubdir(project, sourceSetName, target).first.orNull}
      generatedEntitiesKotlin: ${generatedEntitiesKotlin(project, sourceSetName, target).first.orNull}
      codegenCustomPath: ${ext.codegenCustomPath.orNull}
      codegenIntoPermanentLocation: $codegenIntoPermanentLocation
      """.trimIndent()
    )
  }

  val codegenIntoPermanentLocation: Boolean =
    ext.enableCodegenAI.convention(false).get() || ext.codegenIntoPermanentLocation.convention(false).get()

  fun generatedEntitiesDir(project: Project) =
    if (ext.codegenCustomPath.isPresent) {
      project.layout.dir(ext.codegenCustomPath.map { File(it) }) to EntitiesDirType.Custom
    }
    else if (codegenIntoPermanentLocation)
      // create a static Provider<Directory> from project.projectDir
      project.layout.dir(project.provider<File> { project.projectDir }).map { it.dir("entities") } to EntitiesDirType.Permanent
    else
      project.generatedRootDir.map { it.dir("entities") } to EntitiesDirType.Regular

  fun Directory.dirIfNonEmpty(name: String): Directory =
    if (name.isNotEmpty())
      this.dir(name)
    else
      this

  fun generatedEntitiesSubdir(project: Project, sourceSetName: String, target: String) =
    generatedEntitiesDir(project).let { (dir, type) -> dir.map {
      it.dirIfNonEmpty(target).dirIfNonEmpty(sourceSetName) } to type
    }

  fun generatedEntitiesKotlin(project: Project, sourceSetName: String, target: String) =
    generatedEntitiesSubdir(project, sourceSetName, target).let { (dir, type) -> dir.map { it.dir("kotlin") } to type }
}
