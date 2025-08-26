package io.exoquery.plugin.transform

import io.decomat.Is
import io.exoquery.ParseError
import io.exoquery.TransformXrError
import io.exoquery.config.ExoCompileOptions
import io.exoquery.generation.Code
import io.exoquery.plugin.location
import io.exoquery.plugin.logging.CompileLogger
import io.exoquery.plugin.trees.ContainerExpr
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.kotlinFqName

typealias FileQueryAccum = FileAccum<PrintableQuery>
typealias FileCodegenAccum = FileAccum<Code.Entities>

data class VisitorContext(val symbolSet: SymbolSet, val queriesAccum: FileQueryAccum, val codegenAccum: FileCodegenAccum = FileCodegenAccum.empty()) {
  fun withNewAccum() = VisitorContext(symbolSet, FileQueryAccum.empty(), FileCodegenAccum.empty())
  fun withNewFileAccum(queryAccum: FileQueryAccum, codegenAccum: FileCodegenAccum) = VisitorContext(symbolSet, queryAccum, codegenAccum)

  companion object {
    fun empty() = VisitorContext(SymbolSet.empty, FileAccum.empty())
  }
}

class VisitTransformExpressions(
  private val context: IrPluginContext,
  private val config: CompilerConfiguration,
  private val exoOptions: ExoCompileOptions?
) : IrElementTransformerWithContext<VisitorContext>() {

  fun makeCompileLogger(currentExpr: IrElement) =
    CompileLogger.invoke(config, currentFile, currentExpr)

  fun makeScope(currentExpr: IrElement, scopeOwner: IrSymbol, currentDeclarationParent: IrDeclarationParent?) = CX.Scope(
    currentExpr = currentExpr,
    logger = makeCompileLogger(currentExpr),
    currentFile = currentFile,
    pluginCtx = context,
    compilerConfig = config,
    options = exoOptions,
    scopeOwner = scopeOwner,
    currentDeclarationParent = currentDeclarationParent
  )

  context (CX.Symbology, CX.QueryAccum)
  fun recurseWithAccum(expression: IrExpression): IrExpression =
    when (expression) {
      is IrCall -> visitCall(expression, makeVisitorContext()) as IrExpression
      else -> visitExpression(expression, makeVisitorContext()) as IrExpression
    }

  context (CX.Symbology)
  fun recurse(expression: IrExpression): IrExpression =
    when (expression) {
      is IrCall -> visitCall(expression, makeVisitorContext()) as IrExpression
      else -> visitExpression(expression, makeVisitorContext()) as IrExpression
    }

  context (CX.Symbology)
  fun recurse(expression: IrBlockBody): IrBlockBody =
    visitBody(expression, makeVisitorContext()) as IrBlockBody

  fun recurse(expression: IrExpression, data: VisitorContext): IrExpression =
    when (expression) {
      is IrCall -> visitCall(expression, data) as IrExpression
      else -> visitExpression(expression, data) as IrExpression
    }

  // currentFile is not initialized here yet or something???
  //val sourceFinder: FindSource = FindSource(this.currentFile, exoOptions.projectDir)

  private fun typeIsFqn(type: IrType, fqn: String): Boolean {
    if (type !is IrSimpleType) return false

    return when (val owner = type.classifier.owner) {
      is IrClass -> owner.kotlinFqName.asString() == fqn
      else -> false
    }
  }

//  override fun visitExpression(expression: IrExpression): IrExpression {
//    val compileLogger = CompileLogger(config)
//    compileLogger.warn("---------- Expression Checking:\n" + expression.dumpKotlinLike())
//    return super.visitExpression(expression)
//  }

  override fun visitFileNew(file: IrFile, data: VisitorContext): IrFile {
    // Not sure if we should transfer any symbols from the previous and where they can be. Genrally transfer of symbols
    // should only be cumulative in a captured context for example something like:
    // capture {
    //   people.flatMap { p ->
    //     val foo = "Blah"
    //     addresses.map { a ->
    //       val bar = "Blah"
    //       p.name == foo && a.street == bar
    //     }
    //   }
    // }
    // (which would represent some kind of odd implicit join)
    // It is only in this kind of case that we actually care about propagating identifiers
    // from one nested scope into another in order to know that we don't have a to lift them.
    // It is important to note also that the entire reason we decided to introduce List<IrSymbol> into the scope
    // was in order to be able to do implicit lifting (or at least detect where lifts are needed during the parsing
    // phase where it is easy to make an error and analyze adjacent expressions, the FreeSymbols check at the end
    // of the compilation phases).
    //if (file.hasAnnotation<ExoGoldenTest>())
    val scopeOwner = currentScope!!.scope.scopeOwnerSymbol
    val scope = makeScope(file, scopeOwner, currentDeclarationParent)

    val sanityCheck = currentFile.path == file.path
    if (!sanityCheck) {
      scope.logger.warn("Current file is not the same as the file being visited: ${currentFile.path} != ${file.path}. Will not write this file.")
    }


    val queryAccum = FileQueryAccum(AccumState.RealFile(file))
    val codegenAccum = FileCodegenAccum(AccumState.RealFile(file))
    val ret = super.visitFileNew(file, data.withNewFileAccum(queryAccum, codegenAccum))

    if (sanityCheck && queryAccum.hasQueries() && exoOptions != null) {
      //BuildQueryFile(file, fileScope, config, exoOptions, currentFile).buildRegular()
      val queryFile = QueryFile(file, queryAccum, config, exoOptions)
      with(scope) { QueryFileBuilder.invoke(queryFile) }
    }

    if (sanityCheck && codegenAccum.hasQueries() && exoOptions != null) {
      with(scope) { CodegenFileBuilder(exoOptions).invoke(codegenAccum.currentQueries(), file) }
    }

    return ret
  }

  private fun <R> runInContext(scopeContext: CX.Scope, builderContext: CX.Builder, visitorContext: VisitorContext, runBlock: context (CX.Scope, CX.Builder, CX.Symbology, CX.QueryAccum) () -> R): R =
    runBlock(scopeContext, builderContext, CX.Symbology(visitorContext.symbolSet), CX.QueryAccum(visitorContext.queriesAccum))

  override fun visitFunctionNew(declaration: IrFunction, data: VisitorContext): IrStatement {
    val scopeOwner = currentScope!!.scope.scopeOwnerSymbol
    val scopeContext = makeScope(declaration, scopeOwner, currentDeclarationParent)
    val builderContext = CX.Builder(scopeContext)
    val runner = ScopedRunner(scopeContext, builderContext, data)

    val transformAnnotatedFunction = TransformAnnotatedFunction(this)
    return runner.run(declaration) {
      runInContext(scopeContext, builderContext, data) {
        when {
          transformAnnotatedFunction.matches(declaration) -> transformAnnotatedFunction.transform(declaration)
          else -> super.visitFunctionNew(declaration, data)
        }
      }
    }
  }

  data class ScopedRunner(val scopeContext: CX.Scope, val builderContext: CX.Builder, val visitorContext: VisitorContext) {
    fun <R : IrElement> run(expression: R, block: context (CX.Scope, CX.Builder, CX.Symbology, CX.QueryAccum) () -> R): R =
      try {
        block(scopeContext, builderContext, CX.Symbology(visitorContext.symbolSet), CX.QueryAccum(visitorContext.queriesAccum))
      } catch (e: ParseError) {
        // builderContext.logger.error(e.msg, e.location ?: expression.location(currentFile.fileEntry))
        scopeContext.logger.error(
          // e.msg + "\n---------------- Stack Trace ----------------\n" + // stackTraceToString includes the message
          e.stackTraceToString(),
        )
        expression
      } catch (e: TransformXrError) {
        val location = expression.location(scopeContext.currentFile.fileEntry)
        scopeContext.logger.error("An error occurred during the transformation of the expression: ${e.message}\n" + e.stackTraceToString(), location)
        expression
      }
  }


  // TODO move this to visitGetValue? That would be more efficient but what other things might we wnat to transform?
  override fun visitExpression(expression: IrExpression, data: VisitorContext): IrExpression {
    val scopeOwner = currentScope!!.scope.scopeOwnerSymbol
    val scopeContext = makeScope(expression, scopeOwner, currentDeclarationParent)

    // We should not be transforming containers that have already been transformed. TODO make them all in to one check and don't check if they're case-class-constructor multiple times!
    with (scopeContext) {
      if (ContainerExpr.Pluckable[Is()].matchesAny(expression)) return expression
    }

    val builderContext = CX.Builder(scopeContext)
    val transformProjectCapture = TransformProjectCapture2(this)
    //val transformScaffoldAnnotatedFunctionCall = TransformScaffoldAnnotatedFunctionCall(this, "[ExoQuery: VisitTransformExpressions-VisitExpr]")
    val runner = ScopedRunner(scopeContext, builderContext, data)

    return runner.run(expression) {
      when {
        // If it is a call that needs to be scaffolded need to recurse into it as a priority (it will call superTransformer recursively and transformProjectCapture will be called on SqlQuery/SqlExpression clauses inside)
        //expression is IrCall && transformScaffoldAnnotatedFunctionCall.matches(expression) ->
        //  transformScaffoldAnnotatedFunctionCall.transform(expression)

        transformProjectCapture.matches(expression) ->
          transformProjectCapture.transform(expression) ?: run {
            // In many situations where the capture cannot be projected e.g.
            // val x = if(x) capture { 123 } else capture { 456 } we need to go further into the expression
            // and transform the `capture { ... }` expressions inside into SqlExpession instances. Calling
            // the super-transformer does that.
            super.visitExpression(expression, data)
          }
        else -> super.visitExpression(expression, data)
      }
    }
  }

  override fun visitCall(expression: IrCall, data: VisitorContext): IrElement {
    val scopeOwner = currentScope!!.scope.scopeOwnerSymbol
    val scopeCtx = makeScope(expression, scopeOwner, currentDeclarationParent)
    val stack = RuntimeException()



    with (scopeCtx) {
      // We should not be transforming containers that have already been transformed. TODO make them all in to one check and don't check if they're case-class-constructor multiple times!
      if (ContainerExpr.Pluckable[Is()].matchesAny(expression)) return expression
      // TODO add Expr, Action, BatchAction constructors so as to not recurse into them
    }

    val builderContext = CX.Builder(scopeCtx)

    val transformPrint = TransformPrintSource(this)
    // TODO just for Expression capture or also for Query capture? Probably both
    val transformCapture = TransformCapturedExpression(this)
    val transformCaptureQuery = TransformCapturedQuery(this, "[ExoQuery: VisitTransformExpressions-VisitCall]")
    val transformSelectClause = TransformSelectClause(this)
    val transformCaptureAction = TransformCapturedAction(this)
    val transformCaptureBatchAction = TransformCapturedBatchAction(this)
    // I.e. tranforms the SqlQuery.build call (i.e. the SqlQuery should have already been transformed into an Uprootable before recursing "out" to the .build call
    // or the .build call should have recursed down into it (because it calls the superTransformer on the reciever of the .build call)
    val transformCompileQuery = TransformCompileQuery(this)
    val transformScaffoldAnnotatedFunctionCall = TransformScaffoldAnnotatedFunctionCall(this, "[ExoQuery: VisitTransformExpressions-VisitCall]")
    val transformReadCodegen = TransformCaptureCodegenRead(data.codegenAccum)

    val runner = ScopedRunner(scopeCtx, builderContext, data)

    // Can possibly call the TransformProjectCapture2 here
    // val transformProjectCapture = TransformProjectCapture2(this)
    //val possiblyProjected =
    //  runner.run(expression) {
    //    if (transformProjectCapture.matches(expression))
    //      transformProjectCapture.transform(expression) ?: expression
    //    else
    //      expression
    //  }

    return runner.run(expression) {
      when {
        transformScaffoldAnnotatedFunctionCall.matches(expression) -> transformScaffoldAnnotatedFunctionCall.transform(expression)

        // 1st that that runs here because printed stuff should not be transformed
        // (and this does not recursively transform stuff inside)
        transformPrint.matches(expression) -> transformPrint.transform(expression)
        // NOTE the .matches function should just be a cheap match on the expression, not a full extractionfalse
        transformCapture.matches(expression) -> transformCapture.transform(expression)
        transformCaptureQuery.matches(expression) -> transformCaptureQuery.transform(expression)
        transformSelectClause.matches(expression) -> transformSelectClause.transform(expression)
        transformCaptureAction.matches(expression) -> transformCaptureAction.transform(expression)
        transformCaptureBatchAction.matches(expression) -> transformCaptureBatchAction.transform(expression)
        // Is this an sqlQuery.build(PostgresDialect) call? if yes see if the it is a compile-time query and transform it
        transformCompileQuery.matches(expression) -> transformCompileQuery.transform(expression)
        transformReadCodegen.matches(expression) -> transformReadCodegen.transform(expression)


        //showAnnotations.matches(expression) -> showAnnotations.transform(expression)

        // Want to run interpolator invoke before other things because the result of it is an SqlExpression that will
        // the be re-parsed in the parser if it is inside of a context(EnclosedContext) e.g. Query.map
        else ->
          // No additional data (i.e. Scope-Symbols) to add since none of the transformers was activated
          super.visitCall(expression, data)
      }
    }
  }
}


/** Finds the line and column of [irElement] within this file. */
fun IrFile.locationOf(irElement: IrElement?): CompilerMessageSourceLocation {
  val sourceRangeInfo = fileEntry.getSourceRangeInfo(
    beginOffset = irElement?.startOffset ?: 0,
    endOffset = irElement?.endOffset ?: 0
  )
  return CompilerMessageLocationWithRange.create(
    path = sourceRangeInfo.filePath,
    lineStart = sourceRangeInfo.startLineNumber + 1,
    columnStart = sourceRangeInfo.startColumnNumber + 1,
    lineEnd = sourceRangeInfo.endLineNumber + 1,
    columnEnd = sourceRangeInfo.endColumnNumber + 1,
    lineContent = null
  )!!
}
