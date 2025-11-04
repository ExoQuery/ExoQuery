package io.exoquery.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.annotation.ExoExtras
import io.exoquery.parseError
import io.exoquery.plugin.fullName
import io.exoquery.plugin.hasAnnotation
import io.exoquery.plugin.location
import io.exoquery.plugin.logging.Messages
import io.exoquery.plugin.ownerFunction
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.safeName
import io.exoquery.plugin.source
import io.exoquery.plugin.trees.Elaborate
import io.exoquery.plugin.trees.Ir
import io.exoquery.plugin.trees.Parser
import io.exoquery.plugin.trees.TypeParser
import io.exoquery.plugin.trees.showLineageAdvanced
import io.exoquery.xr.XRType
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.addAnnotations
import org.jetbrains.kotlin.ir.types.impl.toBuilder
import org.jetbrains.kotlin.ir.types.removeAnnotations
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TransformPrintSource(val superTransformer: VisitTransformExpressions) : Transformer<IrCall>() {

  context(CX.Scope, CX.Builder)
  override fun matches(expression: IrCall): Boolean =
    expression.symbol.owner.hasAnnotation<ExoExtras>()

  sealed interface MatchedType {
    data class Multi(val irs: IrBlockBody) : MatchedType
    data class Single(val ir: IrExpression) : MatchedType
  }

  context(CX.Scope, CX.Builder)
  override fun transform(expression: IrCall): IrExpression =
    with(compileLogger) {
      with(makeLifter()) {
        on(expression).match(
          case(Ir.Call.FunctionUntethered1[Is("io.exoquery.printLineage"), Is()]).then { _, expr ->
            (expr as? IrDeclarationReference)?.let { declRef ->
              val line = expr.showLineageAdvanced().map { (heading, elem) ->  Messages.LineageElementDescription(heading, elem) }.joinToString("\n\n")
              compileLogger.warn("========== Printing Lineage of ${expression.source()}\n$line")
              val output = line.lift()

              output
            } ?: parseError("printLineage(..) only works on direct references to declarations, not expressions like:\n${expr.dumpKotlinLike()}\n================ IR ================\n${expr.dumpSimple()}", expr)
          },

          case(Ir.Call.FunctionUntethered0[Is("io.exoquery.printStoredXRs")]).then { _ ->
            val printedScope = storedXRsScope.scoped { storedXRs.printStored() }
            builder.irString(printedScope)
          },

          case(Ir.Call.FunctionUntethered1[Is("io.exoquery.printSource"), Ir.FunctionExpression.withReturnOnlyBlock[Is()]]).then { _, (ret) ->
            transformPrintSource(MatchedType.Single(ret))
          },
          // printExpr(.. { stuff }: IrFunctionExpression  ..): FunctionCall
          case(Ir.Call.FunctionUntethered1[Is("io.exoquery.printSource"), Ir.FunctionExpression.withBlock[Is(), Is()]]).then { _, (_, args) ->
            transformPrintSource(MatchedType.Multi(args))
          },

          case(Ir.Call.FunctionUntethered1[Is("io.exoquery.printSourceBefore"), Ir.FunctionExpression.withReturnOnlyBlock[Is()]]).then { _, (ret) ->
            transformPrintSource(MatchedType.Single(ret), false)
          },
          // printExpr(.. { stuff }: IrFunctionExpression  ..): FunctionCall
          case(Ir.Call.FunctionUntethered1[Is("io.exoquery.printSourceBefore"), Ir.FunctionExpression.withBlock[Is(), Is()]]).then { _, (_, args) ->
            transformPrintSource(MatchedType.Multi(args), false)
          },

          case(Ir.Call.FunctionUntethered0[Is("currentSourceFile")]).then {
            currentFileRaw.path.lift()
          },
          case(Ir.Call.FunctionUntethered1[Is("io.exoquery.elaborateDataClass"), Is()]).thenThis { _, rec ->
            val rootType =
              Parser.scoped {
                TypeParser.ofTypeAt(this.typeArguments.first()!!, this)
              } as XRType.Product
            val pairs =
              Elaborate.invoke(rec, rootType).map { path ->
                val dotPathExpr = path.path.joinToString(".").lift()
                Pair(dotPathExpr, path.invocation).lift({ a -> a }, { b -> b })
              }
            pairs.lift { it }
          },
          case(Ir.Call.FunctionMem0.WithCaller[Is(), Is("ExoGoldenTest")]).then { caller ->
            caller.call("ExoGoldenTestExpr").invoke(currentFileRaw.path.lift())
          },
          case(Ir.Call.FunctionMem0.WithCaller[Is(), Is("ExoGoldenOverride")]).then { caller ->
            caller.call("ExoGoldenOverrideExpr").invoke(currentFileRaw.path.lift())
          }
        )
      }
    }
      ?: parseError("The expression was not a Global Function (with one argument-block)", expression)

  context(CX.Scope, CX.Builder)
  fun transformPrintSource(argsRaw: MatchedType, applySuperTransform: Boolean = true) = run {
    val args = when (argsRaw) {
      is MatchedType.Single ->
        if (applySuperTransform)
          argsRaw.copy(superTransformer.recurse(argsRaw.ir))
        else
          argsRaw
      is MatchedType.Multi ->
        if (applySuperTransform)
          argsRaw.copy(superTransformer.visitBlockBody(argsRaw.irs) as IrBlockBody)
        else
          argsRaw
    }

    val printSourceExpr = pluginCtx
      .referenceFunctions(
        CallableId(FqName("io.exoquery"), Name.identifier("printSourceExpr"))
      ).first()

    val message =
      when (args) {
        is MatchedType.Single -> Messages.PrintingMessageSingle(args.ir, "Single Return")
        is MatchedType.Multi -> Messages.PrintingMessageMulti(args.irs.statements, "Multi Return Statements")
      }

    compileLogger.warn(message)

    with(builder) {
      this.irCall(printSourceExpr).apply {
        // Call the printSourceExpr function. We assume there are no receivers or context-parameters
        arguments[0] = irString(message)
      }
    }
  }

}
