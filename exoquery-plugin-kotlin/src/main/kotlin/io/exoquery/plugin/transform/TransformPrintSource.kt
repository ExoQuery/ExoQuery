package io.exoquery.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.annotation.ExoExtras
import io.exoquery.parseError
import io.exoquery.plugin.hasAnnotation
import io.exoquery.plugin.location
import io.exoquery.plugin.logging.Messages
import io.exoquery.plugin.printing.dumpSimple
import io.exoquery.plugin.trees.Elaborate
import io.exoquery.plugin.trees.Ir
import io.exoquery.plugin.trees.Parser
import io.exoquery.plugin.trees.TypeParser
import io.exoquery.xr.XRType
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TransformPrintSource(val superTransformer: VisitTransformExpressions) : Transformer<IrCall>() {

  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun matches(expression: IrCall): Boolean =
    expression.symbol.owner.hasAnnotation<ExoExtras>()

  sealed interface MatchedType {
    data class Multi(val irs: IrBlockBody) : MatchedType
    data class Single(val ir: IrExpression) : MatchedType
  }

  context(CX.Scope, CX.Builder, CX.Symbology)
  override fun transform(expression: IrCall): IrExpression =
    with(compileLogger) {
      with(makeLifter()) {
        on(expression).match(
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
                TypeParser.ofTypeAt(this.typeArguments.first()!!, this.location())
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
      ?: parseError("Parsing Failed\n================== The expresson was not a Global Function (with one argument-block): ==================\n" + expression.dumpKotlinLike() + "\n--------------------------\n" + expression.dumpSimple())

  context(CX.Scope, CX.Builder, CX.Symbology)
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
