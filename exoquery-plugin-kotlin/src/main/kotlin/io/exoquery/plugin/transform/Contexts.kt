package io.exoquery.plugin.transform

import io.exoquery.annotation.ErrorDetailsEnabled
import io.exoquery.config.ExoCompileOptions
import io.exoquery.config.OutputStringMaker
import io.exoquery.parseError
import io.exoquery.plugin.getAnnotationArgsIfExists
import io.exoquery.plugin.logging.CompileLogger
import io.exoquery.plugin.trees.DynamicsAccum
import io.exoquery.plugin.trees.Lifter
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

context(CX.Builder) fun makeLifter() = Lifter(this@Builder)

object CX {

  data class Parsing(val binds: DynamicsAccum = DynamicsAccum.newEmpty(), val batchAlias: IrValueParameter? = null)

  data class QueryAccum(val accum: FileQueryAccum)

  sealed interface ErrorDetails {
    val color: Boolean
    val stackCount: Int
    val isEnabled: Boolean
    data class Enabled(override val color: Boolean, override val stackCount: Int) : ErrorDetails { override val isEnabled: Boolean = true }
    object Disabled : ErrorDetails { override val color: Boolean = false; override val isEnabled: Boolean = false; override val stackCount: Int = 0 }
  }

  data class PerFileDebugConfig(
    val addParamDescriptions: Boolean = true,
    val errorDetails: ErrorDetails = ErrorDetails.Disabled
  ) {
    companion object {
      fun fromFile(file: IrFile): PerFileDebugConfig = run {
        val exoDetailsEnabled = file.getAnnotationArgsIfExists<ErrorDetailsEnabled>()
        PerFileDebugConfig(
          addParamDescriptions = true,
          errorDetails = run {
            exoDetailsEnabled
              ?.let { args ->
                args.first()?.let { argExpr ->
                  val isColor = argExpr is IrConst && (argExpr as? IrConst)?.value == true

                  val stackCount = args.getOrNull(1)?.let { expr ->
                    if (expr is IrConst) { (expr as IrConst).value as? Int } else ExoCompileOptions.DefaultErrorDetailsStackCount
                  } ?: ExoCompileOptions.DefaultErrorDetailsStackCount

                  ErrorDetails.Enabled(isColor, stackCount)
                } ?: ErrorDetails.Enabled(ExoCompileOptions.DefaultErrorDetailsColor, ExoCompileOptions.DefaultErrorDetailsStackCount)
              }
              ?: ErrorDetails.Disabled
          }
        )
      }
    }
  }

  data class StoredXRsScope(val storedXRs: CompileTimeStoredXRs)

  data class Scope(
    val currentExpr: IrElement,
    val logger: CompileLogger,
    val currentFile: IrFile,
    val pluginCtx: IrPluginContext,
    val compilerConfig: CompilerConfiguration,
    val options: ExoCompileOptions?,
    val scopeOwner: IrSymbol,
    val storedXRsScope: CompileTimeStoredXRsScope,
    val currentDeclarationParent: IrDeclarationParent?,
    val perFileDebugConfig: PerFileDebugConfig = PerFileDebugConfig()
  ) {
    fun currentDeclarationParentOrFail() = currentDeclarationParent ?: parseError("Cannot get parent of the current declaration", currentExpr)

    val outputStringMaker = options?.outputStringMaker ?: OutputStringMaker.Default

    val errorDetailsEnabled = perFileDebugConfig.errorDetails.isEnabled || (options?.enableErrorDetails ?: ExoCompileOptions.DefaultEnableErrorDetails)
    val errorDetailsColor = if (perFileDebugConfig.errorDetails.isEnabled) perFileDebugConfig.errorDetails.color else (options?.errorDetailsColor ?: ExoCompileOptions.DefaultErrorDetailsColor)

    val stackCount = perFileDebugConfig.errorDetails.stackCount ?: options?.errorDetailsStackCount ?: ExoCompileOptions.DefaultErrorDetailsStackCount

    val compileLogger get() = logger
    val typeSystem by lazy {
      val baseContext = IrTypeSystemContextImpl(pluginCtx.irBuiltIns)
      object : IrTypeSystemContext by baseContext {
        override fun TypeConstructorMarker.isError(): Boolean {
          return false
        }
      }
    }

    fun makeBuilderCtx() =
      Builder(this)
  }

  // Need the scope-owner in order to be able to construct lambdas (which is why this is in the builder)
  data class Builder(val scopeContext: Scope) {
    val scopeOwner get() = scopeContext.scopeOwner
    val builder by lazy { DeclarationIrBuilder(scopeContext.pluginCtx, scopeOwner, scopeContext.currentExpr.startOffset, scopeContext.currentExpr.endOffset) }
  }
}


//data class TransformerOrigin(
//  val pluginCtx: IrPluginContext,
//  val config: CompilerConfiguration,
//  val currentFile: IrFile,
//  val parentScopeSymbols: TransformerScope,
//  val options: ExoCompileOptions
//) {
//  fun makeBuilderContext(expr: IrElement, scopeOwner: IrSymbol) =
//    BuilderContext(pluginCtx, config, scopeOwner, currentFile, expr, parentScopeSymbols, options)
//}
