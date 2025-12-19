package io.exoquery.plugin.transform

import io.exoquery.annotation.TracesEnabled
import io.exoquery.parseError
import io.exoquery.parseErrorAtCurrent
import io.exoquery.plugin.getAnnotation
import io.exoquery.plugin.isClass
import io.exoquery.plugin.logging.CompileLogger
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.regularArgs
import io.exoquery.plugin.source
import io.exoquery.plugin.trees.simpleTypeArgs
import io.exoquery.plugin.varargValues
import io.exoquery.util.FilePrintOutputSink
import io.exoquery.util.TraceConfig
import io.exoquery.util.TraceType
import io.exoquery.util.Tracer
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

// TODO also want to get TracesEnabled annotation from the build query itself.
object ComputeEngineTracing {
  context(scope: CX.Scope)
  private fun getFileTraceAnnotations() = scope.currentFile.getTraceAnnotationArgs()

  context(scope: CX.Scope)
  private fun IrAnnotationContainer.getTraceAnnotationArgs() =
    this.getAnnotation<TracesEnabled>()?.regularArgs?.firstOrNull()?.varargValues() ?: emptyList()

  context(scope: CX.Scope, builder: CX.Builder)
  private fun getTraceAnnotations(dialectType: IrType) = run {
    val traceTypesClsRef = getFileTraceAnnotations() + dialectType.getTraceAnnotationArgs()
    val traceTypesNames =
      traceTypesClsRef
        .map { ref -> (ref as? IrClassReference) ?: parseErrorAtCurrent("Invalid Trace Type: ${ref.source() ?: ref.dumpKotlinLike()} was not a class-reference:\n${ref.dumpSimple()}") }
        .mapNotNull { ref ->
          // it's TracesEnabled(KClass<TraceType.Normalizations>, etc...) so we need to take 1st argument from the type
          val shortName = ref.type.simpleTypeArgs.first().classFqName?.shortName()?.asString()
          shortName?.let { TraceType.Companion.fromClassStr(it) }
        }

    traceTypesNames
  }

  context(scope: CX.Scope, builder: CX.Builder)
  operator fun invoke(queryLabel: String?, dialectType: IrType) =
    if (scope.options != null) {
    val traceTypesNames = getTraceAnnotations(dialectType)
    val writeSource =
      if (traceTypesNames.isNotEmpty())
        FilePrintOutputSink.open(scope.options)
      else
        null
    val traceConfig = TraceConfig.empty.copy(traceTypesNames, writeSource ?: Tracer.OutputSink.None, queryLabel)
    traceConfig to writeSource
  } else
    TraceConfig.empty to Tracer.OutputSink.None
}
