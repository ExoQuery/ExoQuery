package io.exoquery.plugin

import io.exoquery.config.ExoCompileOptions
import io.exoquery.plugin.transform.CompileTimeStoredXRs
import io.exoquery.plugin.transform.CompileTimeStoredXRsScope
import io.exoquery.plugin.transform.CompileTimeStoredXRsScope.StorageMode
import io.exoquery.plugin.transform.FileAccum
import io.exoquery.plugin.transform.VisitTransformExpressions
import io.exoquery.plugin.transform.VisitorContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class GenerationExtension(
  private val config: CompilerConfiguration,
  private val messages: MessageCollector,
  private val exoOptions: ExoCompileOptions?
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    // NOTE: It would be a user.name for cases where the Kotlin playground is used. Need to look into what exactly happens in that case
    val scopeXRs =
      CompileTimeStoredXRsScope(
        when {
          // If no exoOptions, we are likely in a non-Gradle environment e.g. the Kotlin Playground, so default to transient storage
          exoOptions == null -> {
            messages.report(
              org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING,
              "ExoCompileOptions not found, defaulting to transient storage for cross-file stored XRs"
            )
            StorageMode.Transient
          }
          // If the user has disabled cross-file storage, use transient storage
          !exoOptions.enableCrossFileStore ->
            StorageMode.Transient
          // Otherwise, use persistent storage in the specified build directory
          else ->
            StorageMode.Persistent(exoOptions.storedBaseDir)
        },
        exoOptions?.sourceSetName ?: "default",
        exoOptions?.parentSourceSetNames ?: listOf()
      )
    moduleFragment
      .transform(
        VisitTransformExpressions(pluginContext, config, scopeXRs, exoOptions),
        VisitorContext(FileAccum.empty())
      )
  }
}
