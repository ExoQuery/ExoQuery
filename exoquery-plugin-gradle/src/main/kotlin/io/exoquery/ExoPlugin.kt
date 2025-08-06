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

val Project.generatedEntitiesDir get() =
  generatedRootDir.map { it.dir("entities") }

fun Project.generatedEntitiesSubdir(sourceSetName: String, target: String) =
  generatedEntitiesDir.map { it.dir("$target/$sourceSetName") }

fun Project.generatedEntitiesKotlin(sourceSetName: String, target: String) =
  generatedEntitiesSubdir(sourceSetName, target).map { it.dir("kotlin") }
