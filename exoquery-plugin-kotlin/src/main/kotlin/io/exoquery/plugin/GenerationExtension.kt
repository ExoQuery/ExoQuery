package io.exoquery.plugin

import io.exoquery.config.ExoCompileOptions
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
    moduleFragment
      .transform(
        VisitTransformExpressions(pluginContext, config, exoOptions),
        VisitorContext(SymbolSet(listOf(), listOf()), FileAccum.empty())
      )
  }
}
