package io.exoquery.plugin.transform

import io.exoquery.plugin.fullName
import io.exoquery.plugin.regularArgs
import io.exoquery.plugin.trees.PT
import io.exoquery.unpackAction
import io.exoquery.unpackExpr
import io.exoquery.unpackQuery
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.visitors.IrTransformer

// Note that if you don't do `.deepCopyWithSymbols()` then the actual Transformer will modify the original tree adding the XR.show into the unpackQuery/unpackExpr calls
// which will obviously fail. It is non-obvious where the DeclarationIrBuilder actually does this.
context(CX.Scope)
public fun IrElement.prepareForPrinting() =
  TransformShowUnpacks(this@Scope).visitElement(
    this.deepCopyWithSymbols(
      // Need to do this or in some cases will get: kotlin.UninitializedPropertyAccessException: lateinit property parent has not been initialized
      (this as? IrCall)?.symbol?.owner?.parent
    ), Unit
  )

context(CX.Scope)
fun IrElement.dumpKotlinLikePretty() = this.dumpKotlinLike() //prepareForPrinting().dumpKotlinLike()


private class TransformShowUnpacks(val scopeContext: CX.Scope) : IrTransformer<Unit>() {
  override fun visitCall(call: IrCall, data: Unit): IrElement =
    if (call.symbol.fullName == PT.io_exoquery_unpackQuery || call.symbol.fullName == PT.io_exoquery_unpackExpr || call.symbol.fullName == PT.io_exoquery_unpackAction) {
      // I don't think this declaration builder has a real scope so it cannot create lambdas (need a proper scope-stack for that) everything else should be fine.
      val builder = DeclarationIrBuilder(scopeContext.pluginCtx, call.symbol, scopeContext.currentExpr.startOffset, scopeContext.currentExpr.endOffset)
      with(builder) {
        val newCall = irCall(call.symbol)
        val newContent =
          try {
            call.regularArgs.firstOrNull()
              ?.let { it as? IrConst }
              ?.value.toString()
              ?.let { encodedValue ->
                if (call.symbol.fullName == PT.io_exoquery_unpackQuery)
                  unpackQuery(encodedValue).show()
                else if (call.symbol.fullName == PT.io_exoquery_unpackExpr)
                  unpackExpr(encodedValue).show()
                else if (call.symbol.fullName == PT.io_exoquery_unpackAction)
                  unpackAction(encodedValue).show()
                else
                  "<ERROR_UNPACKING>"
              } ?: "<ERROR_EXTRACTING>"
          } catch (e: Throwable) {
            "<ERROR_UNPACKING>"
          }

        // Assuming io_exoquery_unpackQuery/io_exoquery_unpackExpr/io_exoquery_unpackAction all take one arg and have no receivers or context-params
        newCall.arguments[0] = irString(newContent)
        newCall
      }
    } else {
      super.visitCall(call, data)
    }
}
