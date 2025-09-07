package io.exoquery.plugin

import io.exoquery.config.ExoCompileOptions
import io.exoquery.plugin.transform.CompileTimeStoredXRs
import io.exoquery.plugin.transform.CompileTimeStoredXRsScope
import io.exoquery.plugin.transform.CompileTimeStoredXRsScope.StorageMode
import io.exoquery.plugin.transform.FileAccum
import io.exoquery.plugin.transform.SymbolSet
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
    val storedXRsDir = exoOptions?.storedBaseDir ?: System.getProperty("user.home") ?: throw IllegalStateException("Cannot determine user home directory for storing compile-time queries")
    val scopeXRs =
      CompileTimeStoredXRsScope(
        storedXRsDir,
        exoOptions?.sourceSetName ?: "default",
        exoOptions?.parentSourceSetNames ?: listOf(),
        if (exoOptions?.enableCrossFileStore == true) StorageMode.Persistent else StorageMode.Transient
      )
    moduleFragment
      .transform(
        VisitTransformExpressions(pluginContext, config, scopeXRs, exoOptions),
        VisitorContext(SymbolSet(listOf(), listOf()), FileAccum.empty())
      )
  }
}
