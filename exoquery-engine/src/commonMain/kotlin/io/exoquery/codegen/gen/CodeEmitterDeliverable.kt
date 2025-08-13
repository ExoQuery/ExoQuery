package io.exoquery.codegen.gen

import io.exoquery.codegen.model.CodeWrapper
import io.exoquery.codegen.model.CodeWrapperType
import io.exoquery.codegen.model.NamingAnnotationType
import io.exoquery.codegen.model.TablePrepared

/**
 * Represents the totality of what is needed by a CodeEmitter to generate code.
 * Some of these things from global settings e.g. the default namespace and package prefix,
 * Others are specific to the tables being emitted.
 */
data class CodeEmitterDeliverable(
  val packagePath: PackagePath,
  val tables: List<TablePrepared>,
  val codeWrapper: CodeWrapper,
  val tableNamingAnnotationType: NamingAnnotationType = NamingAnnotationType.SerialName,
  val columnNamingAnnotationType: NamingAnnotationType = NamingAnnotationType.SerialName,

) {
  fun makeWritePath(basePath: String): RootedPath =
    when (codeWrapper.wrapperType) {
      // When we're generating an object it needs to be:
      // package ${packagePath.prefix}
      // object ${packagePath.extension} { ... }
      is CodeWrapperType.ObjectGen -> RootedPath(basePath) + packagePath.prefix.addFileExtension(".kt")
      // When we're generating a package it needs to be:
      // package ${packagePath.prefix}.${packagePath.extension} // i.e. packagePath.fullPath()
      // data class ...
      is CodeWrapperType.PackageGen if (tables.size == 1) -> RootedPath(basePath) + packagePath.fullPath() + "${tables.first().name}.kt"
      is CodeWrapperType.PackageGen ->
        throw IllegalStateException(
          "Package path deliverable had multiple tables, which is not supported: ${tables.map { it.name }}"
        )
    }
}
