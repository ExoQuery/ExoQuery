package io.exoquery

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import java.io.File

object ExoPlugin {
  val RelativeOutputPath = "generated/exoquery/"
}

val Project.generatedRootDir get() =
  this.layout.buildDirectory.dir(ExoPlugin.RelativeOutputPath)

val Project.generatedSqlDir get() =
  generatedRootDir.map { it.dir("sql") }

class GeneratedEntitiesDirConventions(val ext: ExoQueryGradlePluginExtension, val project: Project) {
  val codegenIntoPermanentLocation: Boolean =
    ext.enableCodegenAI.convention(false).get() || ext.codegenIntoPermanentLocation.convention(false).get()

  fun generatedEntitiesDir(project: Project) =
    if (ext.codegenCustomPath.isPresent) {
      project.layout.dir(ext.codegenCustomPath.map { File(it) })
    }
    else if (codegenIntoPermanentLocation)
      // create a static Provider<Directory> from project.projectDir
      project.layout.dir(project.provider<File> { project.projectDir }).map { it.dir("entities") }
    else
      project.generatedRootDir.map { it.dir("entities") }

  fun generatedEntitiesSubdir(project: Project, sourceSetName: String, target: String) =
    generatedEntitiesDir(project).map { it.dir("$target/$sourceSetName") }

  fun generatedEntitiesKotlin(project: Project, sourceSetName: String, target: String) =
    generatedEntitiesSubdir(project, sourceSetName, target).map { it.dir("kotlin") }
}
