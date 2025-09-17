package io.exoquery.plugin

import com.google.auto.service.AutoService
import io.exoquery.plugin.settings.EXO_OPTIONS
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.ContextIndependentParameterRenderer
import org.jetbrains.kotlin.diagnostics.BackendErrorMessages

@AutoService(CompilerPluginRegistrar::class)
@OptIn(ExperimentalCompilerApi::class)
class Registrar : CompilerPluginRegistrar() {
  override val supportsK2: Boolean get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    val exoOptions = configuration[EXO_OPTIONS]

    IrGenerationExtension.registerExtension(
      GenerationExtension(
        configuration,
        configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY),
        exoOptions?.build()
      )
    )
  }
}

object Diagnostics {
  inline fun <reified P : PsiElement, A> myWarning1(
    positioningStrategy: AbstractSourceElementPositioningStrategy = SourceElementPositioningStrategies.DEFAULT
  ): DiagnosticFactory1DelegateProvider<A> {
    return DiagnosticFactory1DelegateProvider(Severity.INFO, positioningStrategy, P::class, BackendErrors)
  }

  val SQL: KtDiagnosticFactory1<String> by myWarning1<PsiElement, String>()
  val Renderer = object : ContextIndependentParameterRenderer<String> {
    override fun render(obj: String): String {
      return obj
    }
  }

  object KtMessages : BaseDiagnosticRendererFactory() {
    override val MAP =
      KtDiagnosticFactoryToRendererMap("Query Generated") { map ->
        map.put(SQL, "{0}", Renderer)
      }.value
  }

  init {
    BackendErrorMessages.MAP.put(
      SQL, "{0}", Renderer
    )
  }
}
